package ro.ophthacloud.modules.emr.internal;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "consultations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationEntity extends TenantAwareEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "doctor_name", nullable = false)
    private String doctorName;

    @Column(name = "doctor_signature")
    private String doctorSignature;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private ConsultationStatus status = ConsultationStatus.DRAFT;

    @Builder.Default
    @Column(name = "consultation_date", nullable = false)
    private LocalDate consultationDate = LocalDate.now();

    @Column(name = "chief_complaint")
    private String chiefComplaint;

    @Builder.Default
    @Column(name = "sections_completed")
    private Short sectionsCompleted = 0;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "signed_by_id")
    private UUID signedById;
}
