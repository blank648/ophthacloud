package ro.ophthacloud.modules.optical.event;

import java.time.Instant;
import java.util.UUID;

public record OpticalOrderReadyEvent(
    UUID orderId,
    String orderNumber,
    UUID tenantId,
    UUID patientId,
    Instant readyAt
) {
}
