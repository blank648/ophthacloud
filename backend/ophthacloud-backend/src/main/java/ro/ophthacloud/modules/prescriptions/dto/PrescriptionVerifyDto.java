package ro.ophthacloud.modules.prescriptions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Public verification DTO returned by the unauthenticated QR verification endpoint.
 * Contains only the minimum clinical data needed by an optician.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionVerifyDto {
    private String prescriptionNumber;
    private String patientName;
    private String dateOfBirth;
    private String prescriptionType;
    private String status;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String issuedByName;
    private String clinicName;
    private BigDecimal pdBinocular;
    private List<PrescriptionLineDto> lines;
}
