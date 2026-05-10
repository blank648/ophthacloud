package ro.ophthacloud.modules.notifications.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationLogDto(
        UUID id,
        String patientName,
        String channel,
        String status,
        String recipientAddress,
        String subject,
        String bodyPreview,
        Instant sentAt,
        String externalMessageId
) {}
