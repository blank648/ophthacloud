package ro.ophthacloud.modules.investigations.internal;

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
import java.util.UUID;

@Entity
@Table(name = "investigations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestigationEntity extends TenantAwareEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "consultation_id")
    private UUID consultationId;

    @Column(name = "ordered_by_id", nullable = false)
    private UUID orderedById;

    @Column(name = "ordered_by_name", nullable = false, length = 256)
    private String orderedByName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private InvestigationCategoryType category;

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "device", length = 128)
    private String device;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvestigationStatusType status = InvestigationStatusType.ORDERED;

    @Builder.Default
    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt = Instant.now();

    @Column(name = "performed_at")
    private Instant performedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_data", columnDefinition = "jsonb")
    private String resultData;

    @Column(name = "interpretation", columnDefinition = "text")
    private String interpretation;

    @Builder.Default
    @Column(name = "is_urgent", nullable = false)
    private boolean isUrgent = false;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "performed_by_id")
    private UUID performedById;
}
