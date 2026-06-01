package ro.ophthacloud.modules.optical.dto;

import tools.jackson.databind.ObjectMapper;
import lombok.Builder;
import ro.ophthacloud.modules.optical.internal.OpticalOrderEntity;
import ro.ophthacloud.modules.optical.OrderStage;
import ro.ophthacloud.modules.optical.OrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record OpticalOrderDto(
    UUID id,
    String orderNumber,
    UUID patientId,
    UUID prescriptionId,
    UUID consultationId,
    OrderType orderType,
    OrderStage stage,
    UUID assignedToId,
    String assignedToName,
    String labName,
    String labReference,
    Instant sentToLabAt,
    Instant qcPassedAt,
    Instant readyAt,
    Instant completedAt,
    Instant cancelledAt,
    String cancellationReason,
    BigDecimal totalAmount,
    BigDecimal depositPaid,
    String currency,
    String notes,
    QcResultDto qcResult,
    List<OpticalOrderItemDto> items
) {
    public static OpticalOrderDto from(OpticalOrderEntity entity, List<OpticalOrderItemDto> items, ObjectMapper objectMapper) {
        QcResultDto qc = null;
        if (entity.getQcResult() != null && !entity.getQcResult().isBlank()) {
            try {
                qc = objectMapper.readValue(entity.getQcResult(), QcResultDto.class);
            } catch (Exception e) {
                // log error, return null or empty DTO
            }
        }

        return OpticalOrderDto.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .patientId(entity.getPatientId())
                .prescriptionId(entity.getPrescriptionId())
                .consultationId(entity.getConsultationId())
                .orderType(entity.getOrderType())
                .stage(entity.getStage())
                .assignedToId(entity.getAssignedToId())
                .assignedToName(entity.getAssignedToName())
                .labName(entity.getLabName())
                .labReference(entity.getLabReference())
                .sentToLabAt(entity.getSentToLabAt())
                .qcPassedAt(entity.getQcPassedAt())
                .readyAt(entity.getReadyAt())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .cancellationReason(entity.getCancellationReason())
                .totalAmount(entity.getTotalAmount() != null ? entity.getTotalAmount().setScale(2, RoundingMode.HALF_UP) : null)
                .depositPaid(entity.getDepositPaid() != null ? entity.getDepositPaid().setScale(2, RoundingMode.HALF_UP) : null)
                .currency(entity.getCurrency())
                .notes(entity.getNotes())
                .qcResult(qc)
                .items(items)
                .build();
    }
}
