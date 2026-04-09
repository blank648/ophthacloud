package ro.ophthacloud.shared.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.infrastructure.web.RequestContextHelper;
import ro.ophthacloud.shared.security.SecurityUtils;

import java.util.UUID;

/**
 * Service responsible for persisting audit log records.
 * <p>
 * This is the <strong>only</strong> write path for the {@code audit_log} table.
 * Business modules must not inject {@link AuditLogRepository} directly;
 * they must call this service and pass an {@link AuditEntry}.
 * <p>
 * Design decisions:
 * <ul>
 *   <li><strong>Propagation.REQUIRES_NEW</strong> — audit records are persisted in their own
 *       transaction, independent of the calling service's transaction. This guarantees the
 *       audit entry is committed even if the outer transaction is rolled back (e.g., validation
 *       errors). A failed audit write should never abort the business operation.</li>
 *   <li>The service auto-populates {@code tenantId}, {@code userId}, and {@code ipAddress}
 *       from the current security context and the HTTP request — callers supply only
 *       business-level fields via {@link AuditEntry}.</li>
 *   <li>If {@code SecurityUtils} throws (e.g., system/batch contexts), the error is logged
 *       as WARN and the audit record is still written with a {@code SYSTEM} user id
 *       so that batch operations are traceable.</li>
 * </ul>
 *
 * Per GUIDE_05 §7.1 and GUIDE_07 §1.3.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private static final String SYSTEM_USER_ID = "SYSTEM";

    private final AuditLogRepository auditLogRepository;
    private final RequestContextHelper requestContextHelper;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           RequestContextHelper requestContextHelper) {
        this.auditLogRepository  = auditLogRepository;
        this.requestContextHelper = requestContextHelper;
    }

    /**
     * Persists an audit log record in a <em>new</em> transaction.
     * <p>
     * Automatically resolves {@code tenantId}, {@code userId}, and {@code ipAddress}
     * from the current security/request context. Callers supply only the business fields
     * via the {@link AuditEntry} builder.
     *
     * <pre>{@code
     * auditLogService.log(
     *     AuditEntry.builder()
     *         .action("CREATE")
     *         .entityType("Patient")
     *         .entityId(patient.getId())
     *         .changedField("mrn", null, patient.getMrn())
     *         .build()
     * );
     * }</pre>
     *
     * @param entry the audit entry built by the caller — must not be null
     * @throws IllegalArgumentException if entry is null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("AuditEntry must not be null");
        }

        AuditLogEntity entity = buildEntity(entry);
        auditLogRepository.save(entity);

        if (log.isDebugEnabled()) {
            log.debug("Audit logged: action={} entityType={} entityId={} tenantId={} actorId={}",
                    entry.getAction(), entry.getEntityType(), entry.getEntityId(),
                    entity.getTenantId(), entity.getActorId());
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Maps {@link AuditEntry} to {@link AuditLogEntity}, auto-filling context fields.
     */
    private AuditLogEntity buildEntity(AuditEntry entry) {
        AuditLogEntity entity = new AuditLogEntity();

        // --- tenant / user from security context ---
        entity.setTenantId(resolveTenantId());
        entity.setActorId(resolveActorId());

        // --- business fields from the entry ---
        entity.setAction(entry.getAction());
        entity.setEntityType(entry.getEntityType());
        entity.setEntityId(entry.getEntityId());
        entity.setChangedFields(entry.getChangedFields());

        // --- request metadata ---
        entity.setIpAddress(requestContextHelper.currentClientIp());

        return entity;
    }

    /**
     * Resolves the current tenant UUID from the security context.
     * Falls back to {@code null} for system/batch contexts (logged as WARN).
     */
    private UUID resolveTenantId() {
        try {
            String tenantIdStr = SecurityUtils.currentTenantId();
            return UUID.fromString(tenantIdStr);
        } catch (IllegalStateException e) {
            log.warn("AuditLogService: no tenant in SecurityContext — writing audit record without tenantId. " +
                     "This is expected only for system/batch operations.");
            return null;
        }
    }

    /**
     * Resolves the current user's Keycloak ID from the security context.
     * Falls back to {@code "SYSTEM"} for system/batch contexts.
     */
    private String resolveActorId() {
        try {
            return SecurityUtils.currentPrincipal().keycloakUserId();
        } catch (IllegalStateException e) {
            log.warn("AuditLogService: no principal in SecurityContext — using '{}' as actorId.", SYSTEM_USER_ID);
            return SYSTEM_USER_ID;
        }
    }
}
