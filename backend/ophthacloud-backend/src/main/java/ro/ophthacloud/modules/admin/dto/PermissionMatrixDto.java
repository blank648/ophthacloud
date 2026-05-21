package ro.ophthacloud.modules.admin.dto;

import ro.ophthacloud.modules.admin.internal.StaffRole;
import ro.ophthacloud.modules.admin.internal.TenantRoleModulePermissionEntity;

/**
 * DTO for a single row in the permission matrix.
 * Matches GUIDE_04 §10.2 GET response shape.
 */
public record PermissionMatrixDto(
        StaffRole role,
        String moduleCode,
        boolean canView,
        boolean canCreate,
        boolean canEdit,
        boolean canDelete,
        boolean canSign,
        boolean canExport
) {
    public static PermissionMatrixDto from(TenantRoleModulePermissionEntity entity) {
        return new PermissionMatrixDto(
                entity.getRole(),
                entity.getModuleCode(),
                entity.isCanView(),
                entity.isCanCreate(),
                entity.isCanEdit(),
                entity.isCanDelete(),
                entity.isCanSign(),
                entity.isCanExport()
        );
    }
}
