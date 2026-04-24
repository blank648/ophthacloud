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

import java.util.UUID;

@Entity
@Table(name = "clinical_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalTemplateEntity extends TenantAwareEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "section_code", nullable = false, length = 1)
    private String sectionCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_data", nullable = false, columnDefinition = "jsonb")
    private String templateData;

    @Column(name = "category")
    private String category;

    @Builder.Default
    @Column(name = "is_global")
    private Boolean isGlobal = false;

    @Column(name = "created_by_id")
    private UUID createdById;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;
}
