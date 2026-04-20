package ro.ophthacloud.modules.appointments.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import ro.ophthacloud.shared.enums.AppointmentStatus;

/**
 * Request body for PATCH /api/v1/appointments/{id}/status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "status este obligatoriu.")
    private AppointmentStatus status;

    private String cancellationReason;
}
