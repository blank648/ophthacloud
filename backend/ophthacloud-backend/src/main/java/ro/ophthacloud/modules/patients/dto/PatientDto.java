package ro.ophthacloud.modules.patients.dto;

import ro.ophthacloud.modules.patients.internal.PatientEntity;
import ro.ophthacloud.shared.enums.GenderType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

/**
 * Full patient response DTO for GET /api/v1/patients/{id}.
 * Includes nested medicalHistory and statistics sub-objects.
 * Shape matches GUIDE_04 §2.3.
 */
public record PatientDto(
        UUID id,
        String mrn,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        int age,
        GenderType gender,
        String cnp,
        String phone,
        String phoneAlt,
        String email,
        String address,
        String city,
        String county,
        String bloodType,
        String occupation,
        String employer,
        String emergencyContactName,
        String emergencyContactPhone,
        String insuranceProvider,
        String insuranceNumber,
        String referringDoctor,
        boolean hasPortalAccess,
        Instant portalInvitedAt,
        boolean isActive,
        String notes,
        String avatarUrl,
        MedicalHistoryDto medicalHistory,
        Instant createdAt,
        Instant updatedAt
) {
    public static PatientDto from(PatientEntity p) {
        int age = Period.between(p.getDateOfBirth(), LocalDate.now()).getYears();
        MedicalHistoryDto history = (p.getMedicalHistory() != null)
                ? MedicalHistoryDto.from(p.getMedicalHistory())
                : null;
        return new PatientDto(
                p.getId(),
                p.getMrn(),
                p.getFirstName(),
                p.getLastName(),
                p.getDateOfBirth(),
                age,
                p.getGender(),
                p.getCnp(),
                p.getPhone(),
                p.getPhoneAlt(),
                p.getEmail(),
                p.getAddress(),
                p.getCity(),
                p.getCounty(),
                p.getBloodType(),
                p.getOccupation(),
                p.getEmployer(),
                p.getEmergencyContactName(),
                p.getEmergencyContactPhone(),
                p.getInsuranceProvider(),
                p.getInsuranceNumber(),
                p.getReferringDoctor(),
                p.isHasPortalAccess(),
                p.getPortalInvitedAt(),
                p.isActive(),
                p.getNotes(),
                p.getAvatarUrl(),
                history,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
