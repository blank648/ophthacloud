package ro.ophthacloud.modules.appointments.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by {@link ro.ophthacloud.modules.appointments.AppointmentManagementFacade}
 * when a patient is checked in (appointment status → CHECKED_IN).
 * Consumed by the notifications module to notify the doctor that the patient has arrived.
 * Defined in OC-014 / GUIDE_06 §2.4.
 */
public record PatientCheckedInEvent(
        UUID appointmentId,
        UUID patientId,
        UUID tenantId,
        Instant checkedInAt
) {}
