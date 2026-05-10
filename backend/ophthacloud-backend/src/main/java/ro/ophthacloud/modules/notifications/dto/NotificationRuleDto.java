package ro.ophthacloud.modules.notifications.dto;

import java.util.Map;
import java.util.UUID;

public record NotificationRuleDto(
        UUID id,
        String name,
        Map<String, Object> configData,
        boolean isActive
) {}
