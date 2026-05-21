package ro.ophthacloud.modules.admin.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

/**
 * JPA entity for {@code tenant_role_module_permissions} table.
 * Maps the RBAC permission matrix: each row grants specific capabilities
 * (view, create, edit, delete, sign, export) for a given role on a specific module.
 */
@Entity
@Table(name = "tenant_role_module_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantRoleModulePermissionEntity extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 64)
    private StaffRole role;

    @Column(name = "module_code", nullable = false, length = 64)
    private String moduleCode;

    @Builder.Default
    @Column(name = "can_view", nullable = false)
    private boolean canView = false;

    @Builder.Default
    @Column(name = "can_create", nullable = false)
    private boolean canCreate = false;

    @Builder.Default
    @Column(name = "can_edit", nullable = false)
    private boolean canEdit = false;

    @Builder.Default
    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = false;

    @Builder.Default
    @Column(name = "can_sign", nullable = false)
    private boolean canSign = false;

    @Builder.Default
    @Column(name = "can_export", nullable = false)
    private boolean canExport = false;
}
