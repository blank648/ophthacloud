package ro.unibuc.fmi.ophthacloud.gamification.dto;

import ro.unibuc.fmi.ophthacloud.clinicalamsler.api.TargetEye;
import ro.unibuc.fmi.ophthacloud.gamification.domain.AdherenceStatus;

import java.time.Instant;
import java.util.UUID;

public record ScheduledDoseDto(
        UUID planId,
        String medicationName,
        TargetEye targetEye,
        int dropsPerDose,
        Instant scheduledFor,
        String localTime,
        AdherenceStatus status,
        Instant confirmedAt
) {
}
