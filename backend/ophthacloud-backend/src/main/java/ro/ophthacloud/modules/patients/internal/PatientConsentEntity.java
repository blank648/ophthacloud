package ro.ophthacloud.modules.patients.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.time.Instant;

@Entity
@Table(name = "patient_consents")
@Getter
@Setter
@NoArgsConstructor
public class PatientConsentEntity extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @Column(name = "consent_type", nullable = false, length = 64)
    private String consentType;

    @Column(name = "is_granted", nullable = false)
    private boolean isGranted;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "signed_by_name", length = 256)
    private String signedByName;

    @Column(name = "document_url", length = 1024)
    private String documentUrl;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "notes")
    private String notes;
}
