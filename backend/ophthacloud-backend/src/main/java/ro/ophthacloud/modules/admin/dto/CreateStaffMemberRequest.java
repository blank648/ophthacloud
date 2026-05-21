package ro.ophthacloud.modules.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ro.ophthacloud.modules.admin.internal.StaffRole;

public record CreateStaffMemberRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        String phone,
        @NotNull StaffRole role,
        String specialization,
        String licenseNumber,
        boolean sendInviteEmail
) {
}
