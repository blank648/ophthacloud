package ro.ophthacloud.modules.portal.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Appointment view for portal patients.
 * Excludes {@code internalNotes} per GUIDE_06 §10.3.
 */
public record PortalAppointmentDto(
        UUID id,
        String doctorName,
        Instant startAt,
        Instant endAt,
        String status,
        String chiefComplaint,
        String patientNotes,
        String room,
        int durationMinutes
) {}
