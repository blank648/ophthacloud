package ro.ophthacloud.modules.optical.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ro.ophthacloud.modules.optical.internal.OrderStage;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStageRequest {

    @NotNull
    private OrderStage newStage;

    private String cancellationReason;
}
