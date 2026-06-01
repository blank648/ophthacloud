package ro.ophthacloud.modules.optical.dto;

import lombok.Builder;
import ro.ophthacloud.modules.optical.internal.InvoiceEntity;
import ro.ophthacloud.modules.optical.internal.InvoiceLineEntity;
import ro.ophthacloud.modules.optical.internal.InvoiceStatus;
import ro.ophthacloud.modules.optical.internal.PaymentMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record InvoiceDto(
    UUID id,
    String invoiceNumber,
    UUID patientId,
    String patientName,
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
    String pdfPath,
    List<InvoiceLineDto> lines
) {
    public static InvoiceDto from(InvoiceEntity entity) {
        return from(entity, null, null);
    }

    public static InvoiceDto from(InvoiceEntity entity, List<InvoiceLineEntity> lines, String patientName) {
        List<InvoiceLineDto> lineDtos = lines != null ? lines.stream()
                .map(l -> new InvoiceLineDto(
                        l.getId(),
                        l.getDescription(),
                        l.getQuantity(),
                        l.getUnitPrice() != null ? l.getUnitPrice().setScale(2, RoundingMode.HALF_UP) : null,
                        l.getVatRate() != null ? l.getVatRate().setScale(2, RoundingMode.HALF_UP) : null,
                        l.getDiscountPercent() != null ? l.getDiscountPercent().setScale(2, RoundingMode.HALF_UP) : null,
                        l.getLineTotal() != null ? l.getLineTotal().setScale(2, RoundingMode.HALF_UP) : null,
                        l.getServiceItemId()
                ))
                .toList() : List.of();

        return InvoiceDto.builder()
                .id(entity.getId())
                .invoiceNumber(entity.getInvoiceNumber())
                .patientId(entity.getPatientId())
                .patientName(patientName)
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
                .lines(lineDtos)
                .build();
    }
}
