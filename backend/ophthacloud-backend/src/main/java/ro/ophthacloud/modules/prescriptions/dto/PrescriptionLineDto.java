package ro.ophthacloud.modules.prescriptions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionLineDto {
    private String eye;
    private BigDecimal sph;
    private BigDecimal cyl;
    private Short axis;
    private BigDecimal addPower;
    private String vaSc;
    private String vaCc;
    private String bcva;
    private BigDecimal seq;
}
