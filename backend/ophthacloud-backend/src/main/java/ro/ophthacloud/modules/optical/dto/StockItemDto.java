package ro.ophthacloud.modules.optical.dto;

import lombok.Builder;
import ro.ophthacloud.modules.optical.internal.StockItemEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Builder
public record StockItemDto(
    UUID id,
    UUID serviceItemId,
    String name,
    String category,
    String sku,
    String barcode,
    String brand,
    Integer currentStock,
    Integer minimumStock,
    BigDecimal unitCost,
    BigDecimal unitPrice,
    String currency,
    String location,
    Boolean isActive,
    Instant lastRestockedAt
) {
    public static StockItemDto from(StockItemEntity entity) {
        return StockItemDto.builder()
                .id(entity.getId())
                .serviceItemId(entity.getServiceItemId())
                .name(entity.getName())
                .category(entity.getCategory())
                .sku(entity.getSku())
                .barcode(entity.getBarcode())
                .brand(entity.getBrand())
                .currentStock(entity.getCurrentStock())
                .minimumStock(entity.getMinimumStock())
                .unitCost(entity.getUnitCost() != null ? entity.getUnitCost().setScale(2, RoundingMode.HALF_UP) : null)
                .unitPrice(entity.getUnitPrice() != null ? entity.getUnitPrice().setScale(2, RoundingMode.HALF_UP) : null)
                .currency(entity.getCurrency())
                .location(entity.getLocation())
                .isActive(entity.getIsActive())
                .lastRestockedAt(entity.getLastRestockedAt())
                .build();
    }
}
