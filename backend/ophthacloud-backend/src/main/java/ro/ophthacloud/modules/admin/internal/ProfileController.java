package ro.ophthacloud.modules.admin.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ophthacloud.modules.admin.dto.StaffMemberDto;
import ro.ophthacloud.shared.api.ApiResponse;
import ro.ophthacloud.shared.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Profile", description = "Endpoints for the currently logged in user")
public class ProfileController {

    private final StaffMemberRepository staffMemberRepository;
    private final KeycloakAdminService keycloakAdminService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ApiResponse<StaffMemberDto> getMyProfile() {
        String staffIdStr = SecurityUtils.currentStaffId();
        String keycloakUserId = SecurityUtils.currentPrincipal().keycloakUserId();
        
        if (staffIdStr == null || staffIdStr.isEmpty()) {
            throw new IllegalStateException("No staff ID found in current security context");
        }
        
        UUID staffId = UUID.fromString(staffIdStr);
        StaffMemberEntity entity = staffMemberRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found: " + staffId));
                
        // staff_members is the authoritative source for the clinician's name (medical record).
        // We only sync the email from Keycloak (login identity) as a fallback — names always come
        // from the DB so a stale/incorrect Keycloak attribute can never display the wrong person.
        StaffMemberDto dto = StaffMemberDto.from(entity);

        try {
            var kcUser = keycloakAdminService.getUser(keycloakUserId);
            if (kcUser != null && kcUser.getEmail() != null && !kcUser.getEmail().equals(dto.email())) {
                dto = new StaffMemberDto(
                    dto.id(),
                    dto.keycloakUserId(),
                    dto.firstName(),
                    dto.lastName(),
                    kcUser.getEmail(),
                    dto.phone(),
                    dto.role(),
                    dto.specialization(),
                    dto.licenseNumber(),
                    dto.isActive(),
                    dto.avatarUrl(),
                    dto.lastLoginAt(),
                    dto.createdAt(),
                    dto.updatedAt()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Keycloak user data for profile email sync: keycloakUserId={}", keycloakUserId, e);
        }

        return ApiResponse.of(dto);
    }
}
