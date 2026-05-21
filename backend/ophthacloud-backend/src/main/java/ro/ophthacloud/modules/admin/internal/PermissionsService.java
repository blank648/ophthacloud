package ro.ophthacloud.modules.admin.internal;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.admin.dto.PermissionMatrixDto;
import ro.ophthacloud.modules.admin.dto.UpdatePermissionsRequest;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the 4-step permission update cascade from GUIDE_06 §9.2:
 * <ol>
 *   <li>Update {@code tenant_role_module_permissions} rows in DB</li>
 *   <li>Regenerate {@code permissions_json} for all affected users</li>
 *   <li>Update Keycloak user attributes for all users with that role</li>
 *   <li>Invalidate active sessions via Keycloak Session API</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionsService {

    private final TenantRoleModulePermissionRepository permissionRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<PermissionMatrixDto> getPermissionsByRole(StaffRole role) {
        UUID tenantId = TenantContext.require();
        return permissionRepository.findByTenantIdAndRole(tenantId, role).stream()
                .map(PermissionMatrixDto::from)
                .toList();
    }

    /**
     * Executes the 4-step permission cascade per GUIDE_06 §9.2.
     */
    @Transactional
    public List<PermissionMatrixDto> updatePermissions(UpdatePermissionsRequest request) {
        UUID tenantId = TenantContext.require();
        StaffRole role = request.role();

        // Step 1: Replace all permission rows for this role in DB
        permissionRepository.deleteByTenantIdAndRole(tenantId, role);

        List<TenantRoleModulePermissionEntity> newPermissions = request.permissions().stream()
                .map(entry -> TenantRoleModulePermissionEntity.builder()
                        .role(role)
                        .moduleCode(entry.moduleCode())
                        .canView(entry.canView())
                        .canCreate(entry.canCreate())
                        .canEdit(entry.canEdit())
                        .canDelete(entry.canDelete())
                        .canSign(entry.canSign())
                        .canExport(entry.canExport())
                        .build())
                .toList();

        List<TenantRoleModulePermissionEntity> saved = permissionRepository.saveAll(newPermissions);

        // Step 2: Regenerate permissions_json
        String permissionsJson = generatePermissionsJson(saved);

        // Step 3 & 4: Update Keycloak attributes and invalidate sessions for all users with this role
        List<StaffMemberEntity> affectedStaff = staffMemberRepository
                .findByTenantIdAndRole(tenantId, role, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        for (StaffMemberEntity staff : affectedStaff) {
            try {
                keycloakAdminService.setAttributes(
                        staff.getKeycloakUserId(), tenantId, staff.getId(), role, permissionsJson);
                keycloakAdminService.invalidateSessions(staff.getKeycloakUserId());
                log.info("Updated permissions and invalidated sessions for staff {} ({})", staff.getId(), staff.getEmail());
            } catch (Exception e) {
                log.error("Failed to update Keycloak for staff {}: {}", staff.getId(), e.getMessage());
                // Continue with other users — partial failure is logged but doesn't abort
            }
        }

        log.info("Permission cascade completed for role {} in tenant {}. Affected users: {}", role, tenantId, affectedStaff.size());
        return saved.stream().map(PermissionMatrixDto::from).toList();
    }

    /**
     * Generates a JSON string representing the permission set for Keycloak user attributes.
     */
    private String generatePermissionsJson(List<TenantRoleModulePermissionEntity> permissions) {
        Map<String, Map<String, Boolean>> permMap = new HashMap<>();
        for (TenantRoleModulePermissionEntity p : permissions) {
            Map<String, Boolean> modulePerms = new HashMap<>();
            modulePerms.put("view", p.isCanView());
            modulePerms.put("create", p.isCanCreate());
            modulePerms.put("edit", p.isCanEdit());
            modulePerms.put("delete", p.isCanDelete());
            modulePerms.put("sign", p.isCanSign());
            modulePerms.put("export", p.isCanExport());
            permMap.put(p.getModuleCode(), modulePerms);
        }
        try {
            return objectMapper.writeValueAsString(permMap);
        } catch (Exception e) {
            log.error("Failed to serialize permissions JSON", e);
            return "{}";
        }
    }
}
