package ro.ophthacloud.modules.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ro.ophthacloud.modules.admin.internal.StaffRole;

import java.util.List;

/**
 * PUT request body for replacing full permissions for a single role.
 * Matches GUIDE_04 §10.2 PUT contract.
 */
public record UpdatePermissionsRequest(
        @NotNull StaffRole role,
        @NotEmpty @Valid List<PermissionEntry> permissions
) {
    public record PermissionEntry(
            @NotNull String moduleCode,
            boolean canView,
            boolean canCreate,
            boolean canEdit,
            boolean canDelete,
            boolean canSign,
            boolean canExport
    ) {
    }
}
