package ro.ophthacloud.modules.optical.event;

import ro.ophthacloud.modules.optical.internal.OrderType;

import java.time.Instant;
import java.util.UUID;

public record OpticalOrderCreatedEvent(
    UUID orderId,
    String orderNumber,
    UUID tenantId,
    UUID patientId,
    OrderType orderType,
    Instant createdAt
) {
}
