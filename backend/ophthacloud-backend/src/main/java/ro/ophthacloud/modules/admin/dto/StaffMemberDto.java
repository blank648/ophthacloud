package ro.ophthacloud.modules.admin.dto;

import ro.ophthacloud.modules.admin.internal.StaffMemberEntity;
import ro.ophthacloud.modules.admin.internal.StaffRole;

import java.time.Instant;
import java.util.UUID;

public record StaffMemberDto(
        UUID id,
        String keycloakUserId,
        String firstName,
        String lastName,
        String email,
        String phone,
        StaffRole role,
        String specialization,
        String licenseNumber,
        boolean isActive,
        String avatarUrl,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static StaffMemberDto from(StaffMemberEntity entity) {
        return new StaffMemberDto(
                entity.getId(),
                entity.getKeycloakUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getRole(),
                entity.getSpecialization(),
                entity.getLicenseNumber(),
                entity.getIsActive(),
                entity.getAvatarUrl(),
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
