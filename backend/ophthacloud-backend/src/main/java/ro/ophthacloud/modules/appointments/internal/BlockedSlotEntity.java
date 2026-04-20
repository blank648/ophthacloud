package ro.ophthacloud.modules.appointments.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for {@code blocked_slots}.
 * Admin/doctor can block time ranges to prevent new bookings.
 */
@Entity
@Table(name = "blocked_slots")
@Getter
@Setter
@NoArgsConstructor
public class BlockedSlotEntity extends TenantAwareEntity {

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "reason", length = 256)
    private String reason;

    @Column(name = "is_recurring")
    private boolean isRecurring = false;

    /** iCal RRULE string for recurring blocked slots. */
    @Column(name = "recurrence_rule", length = 256)
    private String recurrenceRule;
}
