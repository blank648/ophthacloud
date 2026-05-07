package ro.ophthacloud.modules.optical.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ro.ophthacloud.modules.optical.internal.OrderType;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull
    private UUID patientId;

    private UUID prescriptionId;

    private UUID consultationId;

    @NotNull
    private OrderType orderType;

    @NotNull
    private BigDecimal depositPaid;

    private String notes;
}
