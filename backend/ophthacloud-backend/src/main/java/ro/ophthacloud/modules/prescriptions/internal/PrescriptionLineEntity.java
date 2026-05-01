package ro.ophthacloud.modules.prescriptions.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "prescription_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionLineEntity extends TenantAwareEntity {

    @Column(name = "prescription_id", nullable = false)
    private UUID prescriptionId;

    /**
     * 'OD' (right eye) or 'OS' (left eye).
     */
    @Column(name = "eye", nullable = false, length = 2)
    private String eye;

    @Column(name = "sph", precision = 5, scale = 2)
    private BigDecimal sph;

    @Column(name = "cyl", precision = 5, scale = 2)
    private BigDecimal cyl;

    @Column(name = "axis")
    private Short axis;

    @Column(name = "add_power", precision = 5, scale = 2)
    private BigDecimal addPower;

    @Column(name = "va_sc", length = 16)
    private String vaSc;

    @Column(name = "va_cc", length = 16)
    private String vaCc;

    @Column(name = "bcva", length = 16)
    private String bcva;

    /**
     * SEQ = Sph + Cyl/2, rounded to 0.25D. Auto-computed and stored for audit.
     */
    @Column(name = "seq", precision = 5, scale = 2)
    private BigDecimal seq;
}
