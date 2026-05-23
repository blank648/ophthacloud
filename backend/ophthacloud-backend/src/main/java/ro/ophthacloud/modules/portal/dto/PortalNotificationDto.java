package ro.ophthacloud.modules.portal.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification log entry for portal patients.
 */
public record PortalNotificationDto(
        UUID id,
        String channel,
        String status,
        String subject,
        String bodyPreview,
        Instant sentAt,
        Instant createdAt
) {}
