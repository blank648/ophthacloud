package ro.ophthacloud.modules.emr.dto;

import ro.ophthacloud.modules.emr.internal.ConsultationEntity;
import ro.ophthacloud.modules.emr.internal.ConsultationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lightweight summary DTO used for paginated list endpoints (GUIDE_04 §4.7).
 */
public record ConsultationSummaryDto(
        UUID id,
        UUID patientId,
        UUID doctorId,
        String doctorName,
        ConsultationStatus status,
        LocalDate consultationDate,
        String chiefComplaint,
        int sectionsCompleted,
        Instant signedAt,
        Instant createdAt
) {
    public static ConsultationSummaryDto from(ConsultationEntity e) {
        return new ConsultationSummaryDto(
                e.getId(),
                e.getPatientId(),
                e.getDoctorId(),
                e.getDoctorName(),
                e.getStatus(),
                e.getConsultationDate(),
                e.getChiefComplaint(),
                e.getSectionsCompleted() != null ? e.getSectionsCompleted() : 0,
                e.getSignedAt(),
                e.getCreatedAt()
        );
    }
}
