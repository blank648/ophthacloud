package ro.ophthacloud.modules.patients.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;
import ro.ophthacloud.modules.patients.dto.ActiveDiagnosis;

import java.util.List;

@Entity
@Table(name = "patient_medical_history")
@Getter
@Setter
@NoArgsConstructor
public class PatientMedicalHistoryEntity extends TenantAwareEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @Column(name = "has_diabetes")
    private boolean hasDiabetes = false;

    @Column(name = "has_hypertension")
    private boolean hasHypertension = false;

    @Column(name = "has_cardiovascular")
    private boolean hasCardiovascular = false;

    @Column(name = "has_thyroid")
    private boolean hasThyroid = false;

    @Column(name = "has_autoimmune")
    private boolean hasAutoimmune = false;

    @Column(name = "has_glaucoma_history")
    private boolean hasGlaucomaHistory = false;

    @Column(name = "has_cataract_history")
    private boolean hasCataractHistory = false;

    @Column(name = "has_amd_history")
    private boolean hasAmdHistory = false;

    @Column(name = "has_retinal_detachment")
    private boolean hasRetinalDetachment = false;

    @Column(name = "has_strabismus")
    private boolean hasStrabismus = false;

    @Column(name = "has_previous_eye_surgery")
    private boolean hasPreviousEyeSurgery = false;

    @Column(name = "eye_surgeries_detail")
    private String eyeSurgeriesDetail;

    @Column(name = "known_allergies")
    private String knownAllergies;

    @Column(name = "current_medications")
    private String currentMedications;

    @Column(name = "family_glaucoma")
    private boolean familyGlaucoma = false;

    @Column(name = "family_amd")
    private boolean familyAmd = false;

    @Column(name = "family_other")
    private String familyOther;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "active_diagnoses", columnDefinition = "jsonb")
    private List<ActiveDiagnosis> activeDiagnoses;

    @Column(name = "additional_notes")
    private String additionalNotes;
}
