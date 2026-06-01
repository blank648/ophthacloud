package ro.ophthacloud.modules.optical.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineDto(
    UUID id,
    String description,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal vatRate,
    BigDecimal discountPercent,
    BigDecimal lineTotal,
    UUID serviceItemId
) {}
