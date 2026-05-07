package ro.ophthacloud.modules.optical.dto;

import jakarta.validation.constraints.NotNull;
import ro.ophthacloud.modules.optical.internal.PaymentMethod;

public record PayInvoiceRequest(
    @NotNull
    PaymentMethod paymentMethod,
    String paymentReference
) {
}
