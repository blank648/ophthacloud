package ro.ophthacloud.modules.appointments.event;

import ro.ophthacloud.shared.enums.AppointmentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by {@link ro.ophthacloud.modules.appointments.AppointmentManagementFacade}
 * on every appointment status change.
 * Listeners (notifications module) react via {@code @TransactionalEventListener}.
 * Defined in GUIDE_06 §2.4.
 */
public record AppointmentStatusChangedEvent(
        UUID appointmentId,
        UUID patientId,
        UUID tenantId,
        AppointmentStatus previousStatus,
        AppointmentStatus newStatus,
        Instant changedAt
) {}
