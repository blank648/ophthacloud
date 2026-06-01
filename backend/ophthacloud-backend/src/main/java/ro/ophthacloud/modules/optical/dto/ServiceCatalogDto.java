package ro.ophthacloud.modules.optical.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceCatalogDto(
    UUID id,
    String name,
    String category,
    String sku,
    String brand,
    BigDecimal unitPrice,
    BigDecimal vatRate,
    String currency,
    Boolean isActive,
    String notes
) {
    public static ServiceCatalogDto from(ro.ophthacloud.modules.optical.internal.ServiceCatalogEntity entity) {
        return new ServiceCatalogDto(
            entity.getId(),
            entity.getName(),
            entity.getCategory(),
            entity.getSku(),
            entity.getBrand(),
            entity.getUnitPrice(),
            entity.getVatRate(),
            entity.getCurrency(),
            entity.getIsActive(),
            entity.getNotes()
        );
    }
}
