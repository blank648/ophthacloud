package ro.ophthacloud.modules.appointments.dto;

import ro.ophthacloud.modules.appointments.internal.AppointmentEntity;
import ro.ophthacloud.shared.enums.AppointmentChannel;
import ro.ophthacloud.shared.enums.AppointmentStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Full appointment response DTO (calendar list and single-entity view).
 * endAt is computed at runtime from startAt + durationMinutes (not stored — GUIDE_06 §2.1).
 * patientName and patientMrn are denormalized for calendar performance.
 */
public record AppointmentDto(
        UUID id,
        UUID patientId,
        String patientName,
        String patientMrn,
        String patientAvatarUrl,
        List<String> activeDiagnosisFlags,
        UUID doctorId,
        String doctorName,
        UUID appointmentTypeId,
        String appointmentTypeName,
        String appointmentTypeColor,
        String room,
        int durationMinutes,
        Instant startAt,
        Instant endAt,
        AppointmentStatus status,
        AppointmentChannel channel,
        String chiefComplaint,
        String patientNotes,
        String internalNotes,
        String cancellationReason,
        UUID consultationId,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Builds from entity. patientName/patientMrn/patientAvatarUrl are enriched by the facade
     * after looking up patient data from PatientManagementFacade.
     */
    public static AppointmentDto from(AppointmentEntity a,
                                      String patientName,
                                      String patientMrn,
                                      String patientAvatarUrl,
                                      List<String> activeDiagnosisFlags,
                                      String appointmentTypeName,
                                      String appointmentTypeColor) {
        Instant endAt = a.getStartAt().plus(a.getDurationMinutes(), ChronoUnit.MINUTES);
        return new AppointmentDto(
                a.getId(),
                a.getPatientId(),
                patientName,
                patientMrn,
                patientAvatarUrl,
                activeDiagnosisFlags,
                a.getDoctorId(),
                a.getDoctorName(),
                a.getAppointmentTypeId(),
                appointmentTypeName,
                appointmentTypeColor,
                a.getRoom(),
                a.getDurationMinutes(),
                a.getStartAt(),
                endAt,
                a.getStatus(),
                a.getChannel(),
                a.getChiefComplaint(),
                a.getPatientNotes(),
                a.getInternalNotes(),
                a.getCancellationReason(),
                a.getConsultationId(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
