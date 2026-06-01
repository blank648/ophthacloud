package ro.ophthacloud.modules.optical.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateInvoiceRequest(
    UUID opticalOrderId,
    @NotNull
    UUID patientId,
    List<InvoiceLineRequest> items
) {
}
