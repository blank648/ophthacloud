package ro.ophthacloud.modules.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

public record CreateRecallProtocolRequest(
        @NotBlank String name,
        String icd10Code,
        @Min(1) int recallIntervalMonths,
        String description,
        boolean isActive
) {}
