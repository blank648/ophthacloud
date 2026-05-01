package ro.ophthacloud.modules.investigations.event;

import ro.ophthacloud.modules.investigations.internal.InvestigationCategoryType;

import java.time.Instant;
import java.util.UUID;

public record InvestigationResultAvailableEvent(
    UUID investigationId,
    UUID patientId,
    UUID tenantId,
    InvestigationCategoryType category,
    Instant completedAt
) {
}
