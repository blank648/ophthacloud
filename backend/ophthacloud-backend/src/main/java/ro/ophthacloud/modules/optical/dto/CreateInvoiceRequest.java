package ro.ophthacloud.modules.optical.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateInvoiceRequest(
    @NotNull
    UUID opticalOrderId,
    @NotNull
    UUID patientId
) {
}
