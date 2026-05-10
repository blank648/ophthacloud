package ro.ophthacloud.modules.notifications.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

@Entity
@Table(name = "recall_protocols")
@Getter
@Setter
@NoArgsConstructor
public class RecallProtocolEntity extends TenantAwareEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "icd10_code")
    private String icd10Code;

    @Column(name = "recall_interval_months", nullable = false)
    private Integer recallIntervalMonths;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
