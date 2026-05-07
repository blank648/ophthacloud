package ro.ophthacloud.modules.optical.dto;

import lombok.Builder;
import ro.ophthacloud.modules.optical.internal.InvoiceEntity;
import ro.ophthacloud.modules.optical.internal.InvoiceStatus;
import ro.ophthacloud.modules.optical.internal.PaymentMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Builder
public record InvoiceDto(
    UUID id,
    String invoiceNumber,
    UUID patientId,
    UUID opticalOrderId,
    UUID consultationId,
    InvoiceStatus status,
    Instant issuedAt,
    Instant dueAt,
    Instant paidAt,
    BigDecimal subtotal,
    BigDecimal vatTotal,
    BigDecimal discountTotal,
    BigDecimal total,
    BigDecimal amountPaid,
    String currency,
    PaymentMethod paymentMethod,
    String paymentReference,
    String notes,
    String pdfPath
) {
    public static InvoiceDto from(InvoiceEntity entity) {
        return InvoiceDto.builder()
                .id(entity.getId())
                .invoiceNumber(entity.getInvoiceNumber())
                .patientId(entity.getPatientId())
                .opticalOrderId(entity.getOpticalOrderId())
                .consultationId(entity.getConsultationId())
                .status(entity.getStatus())
                .issuedAt(entity.getIssuedAt())
                .dueAt(entity.getDueAt())
                .paidAt(entity.getPaidAt())
                .subtotal(entity.getSubtotal() != null ? entity.getSubtotal().setScale(2, RoundingMode.HALF_UP) : null)
                .vatTotal(entity.getVatTotal() != null ? entity.getVatTotal().setScale(2, RoundingMode.HALF_UP) : null)
                .discountTotal(entity.getDiscountTotal() != null ? entity.getDiscountTotal().setScale(2, RoundingMode.HALF_UP) : null)
                .total(entity.getTotal() != null ? entity.getTotal().setScale(2, RoundingMode.HALF_UP) : null)
                .amountPaid(entity.getAmountPaid() != null ? entity.getAmountPaid().setScale(2, RoundingMode.HALF_UP) : null)
                .currency(entity.getCurrency())
                .paymentMethod(entity.getPaymentMethod())
                .paymentReference(entity.getPaymentReference())
                .notes(entity.getNotes())
                .pdfPath(entity.getPdfPath())
                .build();
    }
}
