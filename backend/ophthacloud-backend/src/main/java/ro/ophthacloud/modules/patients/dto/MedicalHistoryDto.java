package ro.ophthacloud.modules.patients.dto;

import ro.ophthacloud.modules.patients.dto.ActiveDiagnosis;
import ro.ophthacloud.modules.patients.internal.PatientMedicalHistoryEntity;

import java.util.List;

/**
 * Response DTO carrying the medical history sub-object for GET /api/v1/patients/{id}.
 * Matches the shape defined in GUIDE_04 §2.3.
 */
public record MedicalHistoryDto(
        boolean hasDiabetes,
        boolean hasHypertension,
        boolean hasCardiovascular,
        boolean hasThyroid,
        boolean hasAutoimmune,
        boolean hasGlaucomaHistory,
        boolean hasCataractHistory,
        boolean hasAmdHistory,
        boolean hasRetinalDetachment,
        boolean hasStrabismus,
        boolean hasPreviousEyeSurgery,
        String eyeSurgeriesDetail,
        String knownAllergies,
        String currentMedications,
        boolean familyGlaucoma,
        boolean familyAmd,
        String familyOther,
        List<ActiveDiagnosis> activeDiagnoses,
        String additionalNotes
) {
    public static MedicalHistoryDto from(PatientMedicalHistoryEntity e) {
        return new MedicalHistoryDto(
                e.isHasDiabetes(),
                e.isHasHypertension(),
                e.isHasCardiovascular(),
                e.isHasThyroid(),
                e.isHasAutoimmune(),
                e.isHasGlaucomaHistory(),
                e.isHasCataractHistory(),
                e.isHasAmdHistory(),
                e.isHasRetinalDetachment(),
                e.isHasStrabismus(),
                e.isHasPreviousEyeSurgery(),
                e.getEyeSurgeriesDetail(),
                e.getKnownAllergies(),
                e.getCurrentMedications(),
                e.isFamilyGlaucoma(),
                e.isFamilyAmd(),
                e.getFamilyOther(),
                e.getActiveDiagnoses() != null ? e.getActiveDiagnoses() : List.of(),
                e.getAdditionalNotes()
        );
    }
}
