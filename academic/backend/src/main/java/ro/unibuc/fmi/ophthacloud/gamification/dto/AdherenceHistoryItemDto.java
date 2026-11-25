package ro.unibuc.fmi.ophthacloud.gamification.dto;

import ro.unibuc.fmi.ophthacloud.gamification.domain.AdherenceStatus;

import java.time.Instant;
import java.util.UUID;

public record AdherenceHistoryItemDto(
        UUID planId,
        String medicationName,
        Instant scheduledFor,
        Instant confirmedAt,
        AdherenceStatus status
) {
}
