package ro.ophthacloud.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import ro.ophthacloud.modules.admin.internal.StaffRole;

public record UpdateStaffMemberRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        StaffRole role,
        String specialization,
        String licenseNumber
) {
}
