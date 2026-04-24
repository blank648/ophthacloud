package ro.ophthacloud.modules.emr.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for POST /api/v1/emr/consultations (GUIDE_04 §4.1).
 * doctorId and doctorName are resolved from the security context — not supplied by caller.
 */
public record CreateConsultationRequest(
        UUID patientId,
        UUID appointmentId,
        LocalDate consultationDate,
        String chiefComplaint
) {}
