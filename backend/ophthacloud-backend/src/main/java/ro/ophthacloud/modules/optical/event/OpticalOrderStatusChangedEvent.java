package ro.ophthacloud.modules.optical.event;

import ro.ophthacloud.modules.optical.OrderStage;

import java.time.Instant;
import java.util.UUID;

public record OpticalOrderStatusChangedEvent(
    UUID orderId,
    String orderNumber,
    UUID tenantId,
    UUID patientId,
    OrderStage oldStage,
    OrderStage newStage,
    Instant changedAt
) {
}
