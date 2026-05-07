package ro.ophthacloud.modules.optical.event;

import java.util.UUID;

public record LowStockAlertEvent(
    UUID stockItemId,
    UUID tenantId,
    String itemName,
    String sku,
    int currentStock,
    int minimumStock
) {
}
