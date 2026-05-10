package ro.ophthacloud.modules.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateNotificationRuleRequest(
        @NotBlank String name,
        @NotNull Map<String, Object> configData,
        boolean isActive
) {}
