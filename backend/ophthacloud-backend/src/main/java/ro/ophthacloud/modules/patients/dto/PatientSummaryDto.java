package ro.ophthacloud.modules.patients.dto;

import ro.ophthacloud.modules.patients.dto.ActiveDiagnosis;
import ro.ophthacloud.modules.patients.internal.PatientEntity;
import ro.ophthacloud.shared.enums.GenderType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight summary DTO for the paginated patient list (GET /api/v1/patients).
 * Shape matches GUIDE_04 §2.1.
 */
public record PatientSummaryDto(
        UUID id,
        String mrn,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        int age,
        GenderType gender,
        String phone,
        String email,
        boolean hasPortalAccess,
        boolean isActive,
        List<ActiveDiagnosis> activeDiagnoses,
        Instant createdAt
) {
    public static PatientSummaryDto from(PatientEntity p) {
        int age = Period.between(p.getDateOfBirth(), LocalDate.now()).getYears();
        List<ActiveDiagnosis> diagnoses = (p.getMedicalHistory() != null
                && p.getMedicalHistory().getActiveDiagnoses() != null)
                ? p.getMedicalHistory().getActiveDiagnoses()
                : List.of();
        return new PatientSummaryDto(
                p.getId(),
                p.getMrn(),
                p.getFirstName(),
                p.getLastName(),
                p.getDateOfBirth(),
                age,
                p.getGender(),
                p.getPhone(),
                p.getEmail(),
                p.isHasPortalAccess(),
                p.isActive(),
                diagnoses,
                p.getCreatedAt()
        );
    }
}
