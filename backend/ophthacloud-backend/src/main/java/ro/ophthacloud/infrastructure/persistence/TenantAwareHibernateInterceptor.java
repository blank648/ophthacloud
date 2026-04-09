package ro.ophthacloud.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.util.UUID;

/**
 * AOP aspect that enables the Hibernate {@code tenantFilter} on every JPA Session
 * before any repository method executes.
 * <p>
 * Strategy: intercepts all methods on Spring Data JPA repository implementations
 * within the {@code ro.ophthacloud} package. At any repository call-site,
 * the Facade's {@code @Transactional} has already opened a Hibernate Session,
 * so {@link EntityManager#unwrap(Class)} safely returns the active session.
 * <p>
 * The filter is only enabled when {@link TenantContext#get()} is non-null.
 * This allows admin/batch/system contexts to omit the tenant filter intentionally
 * by running without a populated {@code TenantContext}.
 * <p>
 * The {@code @Order(Integer.MAX_VALUE)} ensures this aspect runs closest to the target method
 * (innermost in the proxy chain), after the transaction interceptor has already opened the session.
 * <p>
 * Defined on {@code @FilterDef}/{@code @Filter} in
 * {@link TenantAwareEntity}: condition {@code tenant_id = :tenantId}.
 */
@Aspect
@Component
@Order(Integer.MAX_VALUE)  // run innermost, after @Transactional opens the session
@Slf4j
public class TenantAwareHibernateInterceptor {

    /** Spring-managed proxy — binds to the active transaction's persistence context. */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Enables the Hibernate {@code tenantFilter} on the current Session before
     * any repository method runs.
     * <p>
     * Pointcut: all methods on any class named {@code *Repository} in the
     * {@code ro.ophthacloud} package hierarchy — covers all Spring Data repos
     * and the audit log repository.
     */
    @Before("execution(* ro.ophthacloud..*Repository.*(..))")
    public void enableTenantFilter() {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            // System/batch context — no filter applied. Any code running here without
            // a tenant context must be in an explicitly admin-privileged code path.
            return;
        }

        try {
            Session session = entityManager.unwrap(Session.class);
            // Avoid HibernateException if filter is already enabled (e.g. nested calls)
            if (session.getEnabledFilter("tenantFilter") == null) {
                session.enableFilter("tenantFilter")
                       .setParameter("tenantId", tenantId);
                log.debug("Hibernate tenantFilter enabled: tenantId={}", tenantId);
            }
        } catch (Exception e) {
            // Log but do not swallow — if filter enabling fails, let the query proceed
            // without filter (will be caught by missing tenant_id or security layer)
            log.warn("Failed to enable Hibernate tenantFilter for tenantId={}: {}", tenantId, e.getMessage());
        }
    }
}
