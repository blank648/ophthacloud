package ro.unibuc.fmi.ophthacloud.gamification.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ConfirmDoseRequest(
        @NotNull UUID planId,
        @NotNull Instant scheduledFor
) {
}
