package ro.ophthacloud.modules.notifications.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.time.Instant;

@Entity
@Table(name = "notification_rules")
@Getter
@Setter
@NoArgsConstructor
public class NotificationRuleEntity extends TenantAwareEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_data", nullable = false, columnDefinition = "jsonb")
    private String configData;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_run_at")
    private Instant lastRunAt;
}
