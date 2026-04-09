package ro.ophthacloud.shared.tenant;

import java.util.UUID;

/**
 * Thread-local storage for the current tenant's ID.
 * <p>
 * The lifecycle is managed by {@code TenantResolutionFilter}:
 * <ol>
 *   <li>Filter calls {@link #set(UUID)} after validating the JWT claim.</li>
 *   <li>All downstream code (service, repository, Hibernate interceptor) calls {@link #get()}.</li>
 *   <li>Filter calls {@link #clear()} in its {@code finally} block.</li>
 * </ol>
 * Never call {@code set()} inside business logic — only the filter owns the lifecycle.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
        // utility class
    }

    /** Sets the current tenant ID for this thread. Called exclusively by {@code TenantResolutionFilter}. */
    public static void set(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }

    /** Returns the current tenant ID, or {@code null} if not set (unauthenticated context). */
    public static UUID get() {
        return TENANT_ID.get();
    }

    /**
     * Returns the current tenant ID, throwing if not set.
     * Used by {@code TenantAwareEntity.assignTenant()} and anywhere tenant is mandatory.
     *
     * @throws IllegalStateException if the tenant context has not been populated
     */
    public static UUID require() {
        UUID id = TENANT_ID.get();
        if (id == null) {
            throw new IllegalStateException(
                    "TenantContext is empty — this operation requires an authenticated tenant context.");
        }
        return id;
    }

    /** Clears the tenant context. Called exclusively by {@code TenantResolutionFilter} in its finally block. */
    public static void clear() {
        TENANT_ID.remove();
    }
}
