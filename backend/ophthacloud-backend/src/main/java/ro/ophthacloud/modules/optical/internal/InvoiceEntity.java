package ro.ophthacloud.modules.optical.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceEntity extends TenantAwareEntity {

    @Column(name = "invoice_number", nullable = false, length = 32)
    private String invoiceNumber;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "optical_order_id")
    private UUID opticalOrderId;

    @Column(name = "consultation_id")
    private UUID consultationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 4)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "vat_total", nullable = false, precision = 12, scale = 4)
    private BigDecimal vatTotal = BigDecimal.ZERO;

    @Column(name = "discount_total", nullable = false, precision = 12, scale = 4)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 4)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 4)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "RON";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 256)
    private String paymentReference;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "pdf_path", length = 1024)
    private String pdfPath;

    @Column(name = "created_by_id")
    private UUID createdById;
}
