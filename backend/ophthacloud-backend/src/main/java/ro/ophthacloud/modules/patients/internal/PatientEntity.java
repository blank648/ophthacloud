package ro.ophthacloud.modules.patients.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;
import ro.ophthacloud.shared.enums.GenderType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
public class PatientEntity extends TenantAwareEntity {

    @Column(name = "mrn", nullable = false, length = 32)
    private String mrn;

    @Column(name = "first_name", nullable = false, length = 128)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 128)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "gender", nullable = false)
    private GenderType gender;

    @Column(name = "cnp", length = 13)
    private String cnp;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "phone_alt", length = 32)
    private String phoneAlt;

    @Column(name = "email", length = 256)
    private String email;

    @Column(name = "address")
    private String address;

    @Column(name = "city", length = 128)
    private String city;

    @Column(name = "county", length = 64)
    private String county;

    @Column(name = "blood_type", length = 8)
    private String bloodType;

    @Column(name = "occupation", length = 128)
    private String occupation;

    @Column(name = "employer", length = 256)
    private String employer;

    @Column(name = "emergency_contact_name", length = 256)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 32)
    private String emergencyContactPhone;

    @Column(name = "insurance_provider", length = 256)
    private String insuranceProvider;

    @Column(name = "insurance_number", length = 128)
    private String insuranceNumber;

    @Column(name = "referring_doctor", length = 256)
    private String referringDoctor;

    @Column(name = "has_portal_access", nullable = false)
    private boolean hasPortalAccess = false;

    @Column(name = "portal_invited_at")
    private Instant portalInvitedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "notes")
    private String notes;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PatientMedicalHistoryEntity medicalHistory;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientConsentEntity> consents = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientAttachmentEntity> attachments = new ArrayList<>();

    public void setMedicalHistory(PatientMedicalHistoryEntity medicalHistory) {
        if (medicalHistory == null) {
            if (this.medicalHistory != null) {
                this.medicalHistory.setPatient(null);
            }
        } else {
            medicalHistory.setPatient(this);
        }
        this.medicalHistory = medicalHistory;
    }
}
