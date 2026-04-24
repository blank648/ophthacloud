package ro.ophthacloud.modules.emr.dto;

import ro.ophthacloud.modules.emr.internal.ConsultationEntity;
import ro.ophthacloud.modules.emr.internal.ConsultationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Full consultation response DTO including all sections keyed by section code.
 * Per GUIDE_04 §4.1 and §4.2.
 */
public record ConsultationDto(
        UUID id,
        UUID patientId,
        UUID appointmentId,
        UUID doctorId,
        String doctorName,
        String doctorSignature,
        ConsultationStatus status,
        LocalDate consultationDate,
        String chiefComplaint,
        int sectionsCompleted,
        UUID signedById,
        Instant signedAt,
        Map<String, ConsultationSectionDto> sections,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Builds a ConsultationDto from entity + pre-built sections map.
     * The sections map is keyed by section code ("A" through "I").
     */
    public static ConsultationDto from(ConsultationEntity e, Map<String, ConsultationSectionDto> sections) {
        return new ConsultationDto(
                e.getId(),
                e.getPatientId(),
                e.getAppointmentId(),
                e.getDoctorId(),
                e.getDoctorName(),
                e.getDoctorSignature(),
                e.getStatus(),
                e.getConsultationDate(),
                e.getChiefComplaint(),
                e.getSectionsCompleted() != null ? e.getSectionsCompleted() : 0,
                e.getSignedById(),
                e.getSignedAt(),
                sections,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
