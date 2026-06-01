package ro.ophthacloud.modules.optical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineRequest(
    UUID serviceItemId,
    @NotBlank String description,
    @NotNull @Positive Integer quantity,
    @NotNull BigDecimal unitPrice, // Gross price (VAT included)
    BigDecimal vatRate
) {}
