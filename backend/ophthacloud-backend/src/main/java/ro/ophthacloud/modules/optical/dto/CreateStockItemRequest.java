package ro.ophthacloud.modules.optical.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateStockItemRequest(
    @NotBlank String name,
    @NotBlank String category,
    String sku,
    String barcode,
    String brand,
    @NotNull @Min(0) Integer currentStock,
    @NotNull @Min(0) Integer minimumStock,
    @NotNull @Min(0) BigDecimal unitCost,
    @NotNull @Min(0) BigDecimal unitPrice,
    String location
) {}
