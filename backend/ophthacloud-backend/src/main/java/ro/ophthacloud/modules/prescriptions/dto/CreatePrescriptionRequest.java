package ro.ophthacloud.modules.prescriptions.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.ophthacloud.modules.prescriptions.enums.LensType;
import ro.ophthacloud.modules.prescriptions.enums.PrescriptionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrescriptionRequest {

    @NotNull(message = "patientId is required")
    private UUID patientId;

    private UUID consultationId;

    @NotNull(message = "prescriptionType is required")
    private PrescriptionType prescriptionType;

    @NotNull(message = "validFrom is required")
    private LocalDate validFrom;

    @NotNull(message = "validUntil is required")
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

    @Valid
    @NotEmpty(message = "At least one prescription line is required")
    private List<PrescriptionLineRequest> lines;
}
