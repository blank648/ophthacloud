package ro.ophthacloud.modules.appointments.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by {@link ro.ophthacloud.modules.appointments.AppointmentManagementFacade}
 * when a new appointment is successfully created (status = BOOKED).
 * Consumed by the notifications module to trigger confirmation messages.
 * Defined in OC-014 / GUIDE_06 §2.4.
 */
public record AppointmentBookedEvent(
        UUID appointmentId,
        UUID patientId,
        UUID tenantId,
        Instant startAt,
        int durationMinutes
) {}
