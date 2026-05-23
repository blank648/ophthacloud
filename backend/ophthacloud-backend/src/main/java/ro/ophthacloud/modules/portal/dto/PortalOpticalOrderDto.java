package ro.ophthacloud.modules.portal.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Optical order status for portal patients.
 */
public record PortalOpticalOrderDto(
        UUID id,
        String orderNumber,
        String stage,
        String orderType,
        Instant createdAt,
        Instant updatedAt
) {}
