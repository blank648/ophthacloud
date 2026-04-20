package ro.ophthacloud.modules.appointments.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;
import ro.ophthacloud.shared.enums.AppointmentChannel;
import ro.ophthacloud.shared.enums.AppointmentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for {@code appointments}.
 * doctor_id and doctor_name are denormalized snapshots — staff entity is managed
 * by the admin module and looked up at booking time.
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class AppointmentEntity extends TenantAwareEntity {

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "appointment_type_id")
    private UUID appointmentTypeId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "doctor_name", nullable = false, length = 256)
    private String doctorName;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "channel", nullable = false)
    private AppointmentChannel channel = AppointmentChannel.IN_PERSON;

    @Column(name = "room", length = 64)
    private String room;

    @Column(name = "chief_complaint")
    private String chiefComplaint;

    @Column(name = "internal_notes")
    private String internalNotes;

    @Column(name = "patient_notes")
    private String patientNotes;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "booked_by_id")
    private UUID bookedById;

    @Column(name = "booked_via", length = 32)
    private String bookedVia = "STAFF";

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "no_show_at")
    private Instant noShowAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** Set when EMR consultation is created for this appointment. */
    @Column(name = "consultation_id")
    private UUID consultationId;
}
