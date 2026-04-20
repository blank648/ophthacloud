package ro.ophthacloud.modules.appointments.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import ro.ophthacloud.shared.enums.AppointmentChannel;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for creating/updating an appointment.
 * Required: patientId, doctorId, startAt, durationMinutes.
 * doctorName must be supplied by caller (denormalized snapshot per GUIDE_03 §6.2).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {

    @NotNull(message = "patientId este obligatoriu.")
    private UUID patientId;

    @NotNull(message = "doctorId este obligatoriu.")
    private UUID doctorId;

    @NotNull(message = "doctorName este obligatoriu.")
    private String doctorName;

    private UUID appointmentTypeId;

    @NotNull(message = "startAt este obligatoriu.")
    private Instant startAt;

    @NotNull(message = "durationMinutes este obligatoriu.")
    @Positive(message = "durationMinutes trebuie să fie pozitiv.")
    private Integer durationMinutes;

    private AppointmentChannel channel;
    private String room;
    private String chiefComplaint;
    private String patientNotes;
    private String internalNotes;
}
