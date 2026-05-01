package ro.ophthacloud.modules.prescriptions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.ophthacloud.modules.prescriptions.internal.LensType;
import ro.ophthacloud.modules.prescriptions.internal.PrescriptionStatusType;
import ro.ophthacloud.modules.prescriptions.internal.PrescriptionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDto {
    private UUID id;
    private UUID patientId;
    private UUID consultationId;
    private String prescriptionNumber;
    private PrescriptionType prescriptionType;
    private PrescriptionStatusType status;
    private UUID issuedById;
    private String issuedByName;
    private Instant issuedAt;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private BigDecimal pdBinocular;
    private BigDecimal pdOd;
    private BigDecimal pdOs;
    private String prismOd;
    private String prismOs;
    private LensType lensType;
    private String lensMaterial;
    private String lensCoating;
    private String frameRecommendation;
    private String clinicalNotes;
    private String patientInstructions;
    private UUID supersededById;
    private String qrCodeToken;
    private String qrVerifyUrl;
    private Instant signedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<PrescriptionLineDto> lines;
}
