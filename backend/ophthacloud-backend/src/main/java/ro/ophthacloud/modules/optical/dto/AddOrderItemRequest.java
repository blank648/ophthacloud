package ro.ophthacloud.modules.optical.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AddOrderItemRequest(
    UUID serviceItemId,
    UUID stockItemId,
    @NotBlank
    String description,
    @Min(1)
    int quantity,
    @NotNull
    BigDecimal unitPrice,
    BigDecimal discountPercent,
    String eye,
    String notes
) {
}
