package ro.ophthacloud.modules.optical.dto;

import lombok.Builder;
import ro.ophthacloud.modules.optical.internal.OpticalOrderItemEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Builder
public record OpticalOrderItemDto(
    UUID id,
    UUID serviceItemId,
    UUID stockItemId,
    String description,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal discountPercent,
    BigDecimal lineTotal,
    String eye,
    String notes
) {
    public static OpticalOrderItemDto from(OpticalOrderItemEntity entity) {
        return OpticalOrderItemDto.builder()
                .id(entity.getId())
                .serviceItemId(entity.getServiceItemId())
                .stockItemId(entity.getStockItemId())
                .description(entity.getDescription())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice() != null ? entity.getUnitPrice().setScale(2, RoundingMode.HALF_UP) : null)
                .discountPercent(entity.getDiscountPercent() != null ? entity.getDiscountPercent().setScale(2, RoundingMode.HALF_UP) : null)
                .lineTotal(entity.getLineTotal() != null ? entity.getLineTotal().setScale(2, RoundingMode.HALF_UP) : null)
                .eye(entity.getEye())
                .notes(entity.getNotes())
                .build();
    }
}
