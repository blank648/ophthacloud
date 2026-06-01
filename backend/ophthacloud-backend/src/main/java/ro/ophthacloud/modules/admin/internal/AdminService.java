package ro.ophthacloud.modules.admin.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.admin.dto.CreateStaffMemberRequest;
import ro.ophthacloud.modules.admin.dto.StaffMemberDto;
import ro.ophthacloud.shared.tenant.TenantContext;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final StaffMemberRepository staffMemberRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final TenantRoleModulePermissionRepository permissionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public StaffMemberDto createStaffMember(CreateStaffMemberRequest request) {
        UUID tenantId = TenantContext.require();

        // Step 1: Create Keycloak User
        String keycloakUserId = keycloakAdminService.createUser(request.email(), request.firstName(), request.lastName());

        try {
            keycloakAdminService.assignTenantMemberRole(keycloakUserId);

            // Step 2: Create staff_member in DB
            StaffMemberEntity entity = StaffMemberEntity.builder()
                    .keycloakUserId(keycloakUserId)
                    .firstName(request.firstName())
                    .lastName(request.lastName())
                    .email(request.email())
                    .phone(request.phone())
                    .role(request.role())
                    .specialization(request.specialization())
                    .licenseNumber(request.licenseNumber())
                    .isActive(true)
                    .build();

            entity = staffMemberRepository.saveAndFlush(entity);

            // Step 3: Set Keycloak attributes
            // Resolve actual configured permissions for the role
            List<TenantRoleModulePermissionEntity> permissions = permissionRepository.findByTenantIdAndRole(tenantId, request.role());
            String permissionsJson = generatePermissionsJson(permissions);
            
            keycloakAdminService.setAttributes(keycloakUserId, tenantId, entity.getId(), request.role(), permissionsJson);

            // Send verification email if requested (best effort)
            if (request.sendInviteEmail()) {
                keycloakAdminService.sendVerifyEmail(keycloakUserId);
            }

            return StaffMemberDto.from(entity);

        } catch (Exception e) {
            log.error("Workflow failed for creating staff member {}. Rolling back Keycloak user.", request.email(), e);
            keycloakAdminService.deleteUser(keycloakUserId);
            throw e;
        }
    }

    private String generatePermissionsJson(List<TenantRoleModulePermissionEntity> permissions) {
        java.util.Map<String, java.util.Map<String, Boolean>> permMap = new java.util.HashMap<>();
        for (TenantRoleModulePermissionEntity p : permissions) {
            java.util.Map<String, Boolean> modulePerms = new java.util.HashMap<>();
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

    @Transactional
    public StaffMemberDto updateStaffMember(UUID staffId, ro.ophthacloud.modules.admin.dto.UpdateStaffMemberRequest request) {
        UUID tenantId = TenantContext.require();
        StaffMemberEntity entity = staffMemberRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found"));

        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setPhone(request.phone());
        if (request.role() != null) {
            entity.setRole(request.role());
        }
        entity.setSpecialization(request.specialization());
        entity.setLicenseNumber(request.licenseNumber());

        entity = staffMemberRepository.save(entity);

        // Update Keycloak attributes (name and role)
        keycloakAdminService.updateUser(entity.getKeycloakUserId(), request.firstName(), request.lastName());

        // We assume default permissions are updated elsewhere, but we update role attribute
        keycloakAdminService.setAttributes(entity.getKeycloakUserId(), tenantId, entity.getId(), entity.getRole(), null);

        log.info("Updated staff member {}", staffId);
        return StaffMemberDto.from(entity);
    }

    @Transactional
    public void deactivateStaffMember(UUID staffId) {
        StaffMemberEntity entity = staffMemberRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found"));

        entity.setIsActive(false);
        staffMemberRepository.save(entity);

        keycloakAdminService.disableUser(entity.getKeycloakUserId());
        keycloakAdminService.invalidateSessions(entity.getKeycloakUserId());
        
        log.info("Deactivated staff member {}", staffId);
    }
}
