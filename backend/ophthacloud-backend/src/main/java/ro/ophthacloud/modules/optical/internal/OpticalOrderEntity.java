package ro.ophthacloud.modules.optical.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "optical_orders")
@Getter
@Setter
@NoArgsConstructor
public class OpticalOrderEntity extends TenantAwareEntity {

    @Column(name = "order_number", nullable = false, length = 32)
    private String orderNumber;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "prescription_id")
    private UUID prescriptionId;

    @Column(name = "consultation_id")
    private UUID consultationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType = OrderType.GLASSES;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private OrderStage stage = OrderStage.RECEIVED;

    @Column(name = "assigned_to_id")
    private UUID assignedToId;

    @Column(name = "assigned_to_name", length = 256)
    private String assignedToName;

    @Column(name = "lab_name", length = 256)
    private String labName;

    @Column(name = "lab_reference", length = 128)
    private String labReference;

    @Column(name = "sent_to_lab_at")
    private Instant sentToLabAt;

    @Column(name = "qc_passed_at")
    private Instant qcPassedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "deposit_paid", nullable = false, precision = 12, scale = 4)
    private BigDecimal depositPaid = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "RON";

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "internal_notes", columnDefinition = "text")
    private String internalNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "qc_result", columnDefinition = "jsonb")
    private String qcResult;

    @Column(name = "created_by_id")
    private UUID createdById;
}
