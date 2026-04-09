package ro.ophthacloud.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.util.UUID;

/**
 * Base class for all tenant-scoped JPA entities.
 * <p>
 * Extends {@link BaseEntity} and adds:
 * <ul>
 *   <li>{@code tenant_id} — set automatically on persist from {@link TenantContext} — never updatable</li>
 *   <li>Hibernate {@code @Filter("tenantFilter")} — appends {@code WHERE tenant_id = :tenantId} to all queries
 *       when the filter is enabled by {@code TenantAwareHibernateInterceptor}</li>
 * </ul>
 * <p>
 * The filter is enabled on every Hibernate {@code Session} by {@code TenantAwareHibernateInterceptor}.
 * If the filter is not enabled (e.g., in system/batch contexts), queries will return data across all tenants —
 * this is intentional for admin background jobs only.
 */
@MappedSuperclass
@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(name = "tenantId", type = UUID.class)
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@NoArgsConstructor
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /**
     * Automatically assigns the tenant ID from {@link TenantContext} before the entity is persisted.
     * Throws {@link IllegalStateException} if no tenant context is set — which means the code path
     * that triggered the persist is unauthenticated, which is a programming error.
     */
    @PrePersist
    void assignTenant() {
        this.tenantId = TenantContext.require();
    }
}
