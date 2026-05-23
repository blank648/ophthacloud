package ro.ophthacloud.modules.admin.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.shared.api.PagedApiResponse;
import ro.ophthacloud.shared.audit.AuditLogEntity;
import ro.ophthacloud.shared.audit.AuditLogRepository;
import ro.ophthacloud.shared.security.SecurityUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Read-only REST controller for audit log viewing.
 * Adheres to GUIDE_04 §11: GET only, paginated, filterable.
 * <p>
 * Supported filters (independently or combined):
 * <ul>
 *   <li>{@code entityType} + optional {@code entityId} — entity trail</li>
 *   <li>{@code userId} — actor-scoped trail</li>
 *   <li>{@code from} + {@code to} — time-range query</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/audit/log")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin / Audit Log", description = "Endpoints for viewing system audit logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasPermission('admin', 'MODULE', 'VIEW')")
    @Operation(summary = "Get audit log entries")
    public PagedApiResponse<AuditLogDto> getAuditLog(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        log.debug("GET /api/v1/audit/log [entityType={}, entityId={}, userId={}, action={}, from={}, to={}]",
                entityType, entityId, userId, action, from, to);

        Page<AuditLogEntity> page;

        if (entityType != null && entityId != null) {
            // Most specific: entity type + ID
            page = auditLogRepository.findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId, pageable);
        } else if (entityType != null) {
            // Entity type only
            page = auditLogRepository.findByTenantIdAndEntityType(tenantId, entityType, pageable);
        } else if (userId != null) {
            // Actor-scoped
            page = auditLogRepository.findByUser(tenantId, userId, pageable);
        } else if (from != null && to != null) {
            // Time-range
            Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            page = auditLogRepository.findByTenantIdAndTimeRange(tenantId, fromInstant, toInstant, pageable);
        } else {
            // No filter — all tenant audit records
            page = auditLogRepository.findByTenantId(tenantId, pageable);
        }

        return PagedApiResponse.of(page.map(AuditLogDto::from));
    }

    /**
     * DTO for audit log entries in the REST response.
     * Matches GUIDE_04 §11 response shape.
     */
    public record AuditLogDto(
            UUID id,
            String actorId,
            String action,
            String entityType,
            UUID entityId,
            Object changedFields,
            String ipAddress,
            Instant occurredAt
    ) {
        public static AuditLogDto from(AuditLogEntity entity) {
            return new AuditLogDto(
                    entity.getId(),
                    entity.getActorId(),
                    entity.getAction(),
                    entity.getEntityType(),
                    entity.getEntityId(),
                    entity.getChangedFields(),
                    entity.getIpAddress(),
                    entity.getCreatedAt()
            );
        }
    }
}
