package ro.ophthacloud.modules.prescriptions.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionEntity extends TenantAwareEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "consultation_id")
    private UUID consultationId;

    @Column(name = "prescription_number", nullable = false, length = 32)
    private String prescriptionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "prescription_type", nullable = false)
    private PrescriptionType prescriptionType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PrescriptionStatusType status = PrescriptionStatusType.ACTIVE;

    @Column(name = "issued_by_id", nullable = false)
    private UUID issuedById;

    @Column(name = "issued_by_name", nullable = false, length = 256)
    private String issuedByName;

    @Builder.Default
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    @Builder.Default
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom = LocalDate.now();

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "pd_binocular", precision = 5, scale = 1)
    private BigDecimal pdBinocular;

    @Column(name = "pd_od", precision = 5, scale = 1)
    private BigDecimal pdOd;

    @Column(name = "pd_os", precision = 5, scale = 1)
    private BigDecimal pdOs;

    @Column(name = "prism_od", length = 64)
    private String prismOd;

    @Column(name = "prism_os", length = 64)
    private String prismOs;

    @Enumerated(EnumType.STRING)
    @Column(name = "lens_type")
    private LensType lensType;

    @Column(name = "lens_material", length = 128)
    private String lensMaterial;

    @Column(name = "lens_coating", length = 256)
    private String lensCoating;

    @Column(name = "frame_recommendation", columnDefinition = "text")
    private String frameRecommendation;

    @Column(name = "clinical_notes", columnDefinition = "text")
    private String clinicalNotes;

    @Column(name = "patient_instructions", columnDefinition = "text")
    private String patientInstructions;

    @Column(name = "superseded_by_id")
    private UUID supersededById;

    /**
     * QR code token — UUID only, per user specification.
     * Generated as UUID.randomUUID() at prescription creation time.
     */
    @Builder.Default
    @Column(name = "qr_code_token", nullable = false)
    private UUID qrCodeToken = UUID.randomUUID();

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "pdf_path", length = 1024)
    private String pdfPath;
}
