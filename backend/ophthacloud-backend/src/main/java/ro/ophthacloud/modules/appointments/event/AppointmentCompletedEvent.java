package ro.ophthacloud.modules.appointments.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by {@link ro.ophthacloud.modules.appointments.AppointmentManagementFacade}
 * when an appointment transitions to COMPLETED status.
 * Consumed by the notifications module and, in future sprints, by the reports module.
 * Defined in OC-014 / GUIDE_06 §2.4.
 */
public record AppointmentCompletedEvent(
        UUID appointmentId,
        UUID patientId,
        UUID tenantId,
        Instant completedAt
) {}
