package ro.ophthacloud.modules.admin.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.admin.dto.CreateStaffMemberRequest;
import ro.ophthacloud.modules.admin.dto.StaffMemberDto;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final StaffMemberRepository staffMemberRepository;
    private final KeycloakAdminService keycloakAdminService;

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
            // Default permissions as empty JSON object for now until permission matrix is implemented
            String defaultPermissionsJson = "{}";
            keycloakAdminService.setAttributes(keycloakUserId, tenantId, entity.getId(), request.role(), defaultPermissionsJson);

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
