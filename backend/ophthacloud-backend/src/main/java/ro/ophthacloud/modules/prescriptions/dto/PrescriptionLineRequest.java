package ro.ophthacloud.modules.prescriptions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionLineRequest {

    @NotBlank(message = "eye is required (OD or OS)")
    private String eye;

    private BigDecimal sph;
    private BigDecimal cyl;
    private Short axis;
    private BigDecimal addPower;
    private String vaSc;
    private String vaCc;
    private String bcva;
}
