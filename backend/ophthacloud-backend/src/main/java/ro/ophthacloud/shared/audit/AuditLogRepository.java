package ro.ophthacloud.shared.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AuditLogEntity}.
 * <p>
 * All write operations go through {@link AuditLogService} — this repository
 * is package-visible intentionally; external modules must use the service.
 * <p>
 * Queries bypass the Hibernate tenant filter because {@code AuditLogEntity}
 * does NOT extend {@code TenantAwareEntity} — tenant isolation is enforced
 * manually via the {@code tenantId} predicate in every query method.
 */
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    /**
     * Returns all audit records for a given tenant, ordered newest-first.
     *
     * @param tenantId the tenant UUID (from {@code SecurityUtils.currentTenantId()})
     * @param pageable pagination parameters
     * @return page of audit log entries
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE a.tenantId = :tenantId ORDER BY a.createdAt DESC")
    Page<AuditLogEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Returns the full audit trail for a specific entity (e.g., a patient, a consultation).
     *
     * @param tenantId   the owning tenant
     * @param entityType simple class name (e.g. {@code "Patient"})
     * @param entityId   the entity's UUID
     * @return list ordered newest-first
     */
    @Query("""
            SELECT a FROM AuditLogEntity a
            WHERE a.tenantId   = :tenantId
              AND a.entityType = :entityType
              AND a.entityId   = :entityId
            ORDER BY a.createdAt DESC
            """)
    List<AuditLogEntity> findByEntity(
            @Param("tenantId")   UUID   tenantId,
            @Param("entityType") String entityType,
            @Param("entityId")   UUID   entityId
    );

    /**
     * Returns all audit records for a specific actor (user) within a tenant.
     *
     * @param tenantId the owning tenant
     * @param actorId  Keycloak user ID string (the {@code sub} claim)
     * @param pageable pagination parameters
     * @return page of audit log entries ordered newest-first
     */
    @Query("""
            SELECT a FROM AuditLogEntity a
            WHERE a.tenantId = :tenantId
              AND a.actorId  = :actorId
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLogEntity> findByUser(
            @Param("tenantId") UUID   tenantId,
            @Param("actorId")  String actorId,
            Pageable pageable
    );

    /**
     * Time-range audit query for reporting (e.g., daily/weekly audit reports).
     *
     * @param tenantId the owning tenant
     * @param from     inclusive start timestamp
     * @param to       exclusive end timestamp
     * @param pageable pagination parameters
     * @return page of audit log entries ordered newest-first
     */
    @Query("""
            SELECT a FROM AuditLogEntity a
            WHERE a.tenantId  = :tenantId
              AND a.createdAt >= :from
              AND a.createdAt <  :to
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLogEntity> findByTenantIdAndTimeRange(
            @Param("tenantId") UUID    tenantId,
            @Param("from")     Instant from,
            @Param("to")       Instant to,
            Pageable pageable
    );
}
