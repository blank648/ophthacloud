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
import java.util.Optional;
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
    private final StaffMemberRepository staffMemberRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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
            page = auditLogRepository.findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId, pageable);
        } else if (entityType != null) {
            page = auditLogRepository.findByTenantIdAndEntityType(tenantId, entityType, pageable);
        } else if (userId != null) {
            page = auditLogRepository.findByUser(tenantId, userId, pageable);
        } else if (action != null) {
            page = auditLogRepository.findByTenantIdAndAction(tenantId, action, pageable);
        } else if (from != null && to != null) {
            Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            page = auditLogRepository.findByTenantIdAndTimeRange(tenantId, fromInstant, toInstant, pageable);
        } else {
            page = auditLogRepository.findByTenantId(tenantId, pageable);
        }

        return PagedApiResponse.of(page.map(entity -> {
            ActorDetails details = resolveActorDetails(tenantId, entity.getActorId());
            return new AuditLogDto(
                    entity.getId(),
                    entity.getActorId(),
                    details.name(),
                    details.role(),
                    entity.getAction(),
                    entity.getEntityType(),
                    entity.getEntityId(),
                    entity.getChangedFields(),
                    entity.getIpAddress(),
                    entity.getCreatedAt()
            );
        }));
    }

    private record ActorDetails(String name, String role) {}

    private ActorDetails resolveActorDetails(UUID tenantId, String actorId) {
        if ("SYSTEM".equalsIgnoreCase(actorId)) {
            return new ActorDetails("Sistem", "System");
        }
        
        try {
            // Check if actorId is a valid UUID representing a staff member or patient
            UUID actorUuid = UUID.fromString(actorId);
            
            // Try looking up in the staff_members table
            Optional<StaffMemberEntity> staffOpt = staffMemberRepository.findById(actorUuid);
            if (staffOpt.isPresent()) {
                StaffMemberEntity s = staffOpt.get();
                return new ActorDetails(s.getFirstName() + " " + s.getLastName(), s.getRole().name());
            }
            
            // Try looking up in the patients table
            try {
                String patientName = jdbcTemplate.queryForObject(
                    "SELECT first_name || ' ' || last_name FROM patients WHERE id = ?",
                    String.class,
                    actorUuid
                );
                if (patientName != null) {
                    return new ActorDetails(patientName, "PATIENT");
                }
            } catch (Exception ignored) {}
            
        } catch (IllegalArgumentException e) {
            // Not a UUID, check if it matches a Keycloak user ID
            Optional<StaffMemberEntity> staffOpt = staffMemberRepository.findByTenantIdAndKeycloakUserId(tenantId, actorId);
            if (staffOpt.isPresent()) {
                StaffMemberEntity s = staffOpt.get();
                return new ActorDetails(s.getFirstName() + " " + s.getLastName(), s.getRole().name());
            }
        }
        
        return new ActorDetails(actorId, "UNKNOWN");
    }

    /**
     * DTO for audit log entries in the REST response.
     * Matches GUIDE_04 §11 response shape with human-readable name and role.
     */
    public record AuditLogDto(
            UUID id,
            String actorId,
            String actorName,
            String actorRole,
            String action,
            String entityType,
            UUID entityId,
            Object changedFields,
            String ipAddress,
            Instant occurredAt
    ) {}
}
