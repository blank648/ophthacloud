package ro.ophthacloud.modules.appointments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * Request DTO for creating and updating appointment types.
 * GUIDE_04 §3.8.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentTypeRequest {

    @NotBlank(message = "name este obligatoriu.")
    private String name;

    private String colorHex;

    @Positive
    private Integer durationMinutes;

    private String description;
}
