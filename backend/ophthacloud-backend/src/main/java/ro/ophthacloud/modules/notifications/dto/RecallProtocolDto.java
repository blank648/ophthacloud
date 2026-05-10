package ro.ophthacloud.modules.notifications.dto;

import java.util.UUID;

public record RecallProtocolDto(
        UUID id,
        String name,
        String icd10Code,
        int recallIntervalMonths,
        String description,
        boolean isActive
) {}
