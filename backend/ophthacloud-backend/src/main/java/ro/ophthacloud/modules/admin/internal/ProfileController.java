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
                
        StaffMemberDto dto = StaffMemberDto.from(entity);
        
        // Merge Keycloak profile data to ensure it always matches login credentials perfectly
        try {
            log.info("Profile sync check: keycloakUserId={}, staffId={}", keycloakUserId, staffId);
            var kcUser = keycloakAdminService.getUser(keycloakUserId);
            if (kcUser != null) {
                dto = new StaffMemberDto(
                    dto.id(),
                    dto.keycloakUserId(),
                    kcUser.getFirstName() != null ? kcUser.getFirstName() : dto.firstName(),
                    kcUser.getLastName() != null ? kcUser.getLastName() : dto.lastName(),
                    kcUser.getEmail() != null ? kcUser.getEmail() : dto.email(),
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
            log.warn("Failed to fetch Keycloak user data for profile sync: keycloakUserId={}", keycloakUserId, e);
        }
                
        return ApiResponse.of(dto);
    }
}
