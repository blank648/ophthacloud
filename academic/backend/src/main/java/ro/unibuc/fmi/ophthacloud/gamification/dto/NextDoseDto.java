package ro.unibuc.fmi.ophthacloud.gamification.dto;

import ro.unibuc.fmi.ophthacloud.clinicalamsler.api.TargetEye;

import java.time.Instant;
import java.util.UUID;

public record NextDoseDto(
        UUID planId,
        String medicationName,
        TargetEye targetEye,
        int dropsPerDose,
        Instant scheduledFor,
        String localTime
) {
}
