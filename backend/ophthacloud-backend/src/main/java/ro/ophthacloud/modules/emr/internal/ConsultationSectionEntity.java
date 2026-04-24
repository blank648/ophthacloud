package ro.ophthacloud.modules.emr.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "consultation_sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationSectionEntity extends TenantAwareEntity {

    @Column(name = "consultation_id", nullable = false)
    private UUID consultationId;

    @Column(name = "section_code", nullable = false, length = 1)
    private String sectionCode;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_data", nullable = false, columnDefinition = "jsonb")
    private String sectionData = "{}";

    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "template_id")
    private UUID templateId;
}
