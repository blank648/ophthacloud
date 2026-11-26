package ro.unibuc.fmi.ophthacloud.patientbooking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@DynamicUpdate
@Table(name = "patients")
public class PatientEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 13, unique = true)
    private String cnp;
    @Column(nullable = false, length = 80)
    private String lastName;
    @Column(nullable = false, length = 80)
    private String firstName;
    @Column(length = 20)
    private String phone;
    @Column(nullable = false)
    private LocalDate birthDate;
    @Column(nullable = false, length = 16)
    private String sex;
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal uiFontScale;
    @Column(nullable = false)
    private boolean highContrast;

    public UUID getId() {
        return id;
    }

    public BigDecimal getUiFontScale() {
        return uiFontScale;
    }

    public boolean isHighContrast() {
        return highContrast;
    }

    public void updatePreferences(BigDecimal scale, boolean contrast) {
        this.uiFontScale = scale;
        this.highContrast = contrast;
    }
}
