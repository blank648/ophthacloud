package ro.unibuc.fmi.ophthacloud.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.unibuc.fmi.ophthacloud.gamification.domain.AdherenceStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "adherence_logs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_adherence_logs_plan_scheduled",
                columnNames = {"plan_id", "scheduled_for"}
        )
)
public class AdherenceLogEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdherenceStatus status;

    protected AdherenceLogEntity() {
    }

    public AdherenceLogEntity(
            UUID id,
            UUID planId,
            Instant scheduledFor,
            Instant confirmedAt,
            AdherenceStatus status
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.planId = planId;
        this.scheduledFor = scheduledFor;
        this.confirmedAt = confirmedAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
    }

    public Instant getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(Instant scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public AdherenceStatus getStatus() {
        return status;
    }

    public void setStatus(AdherenceStatus status) {
        this.status = status;
    }
}
