package ro.ophthacloud.modules.portal.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Investigation result summary for portal patients (COMPLETED investigations only).
 */
public record PortalInvestigationDto(
        UUID id,
        String category,
        String status,
        String orderedByName,
        Instant orderedAt,
        Instant completedAt,
        String resultSummary
) {}
