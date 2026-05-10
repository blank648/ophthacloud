package ro.ophthacloud.modules.optical.internal;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import ro.ophthacloud.modules.optical.dto.*;
import ro.ophthacloud.modules.optical.event.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpticalOrderService {

    private final OpticalOrderRepository orderRepository;
    private final OpticalOrderItemRepository itemRepository;
    private final OrderNumberGenerator numberGenerator;
    private final OrderTotalCalculator totalCalculator;
    private final StockService stockService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public OpticalOrderDto createOrder(UUID tenantId, CreateOrderRequest request, UUID createdById) {
        OpticalOrderEntity order = new OpticalOrderEntity();
        order.setPatientId(request.getPatientId());
        order.setPrescriptionId(request.getPrescriptionId());
        order.setConsultationId(request.getConsultationId());
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : OrderType.GLASSES);
        order.setStage(OrderStage.RECEIVED);
        order.setDepositPaid(request.getDepositPaid());
        order.setNotes(request.getNotes());
        order.setCreatedById(createdById);

        // Generate number based on tenant and year
        order.setOrderNumber(numberGenerator.generate(tenantId, Instant.now()));

        order = orderRepository.save(order);

        eventPublisher.publishEvent(new OpticalOrderCreatedEvent(
                order.getId(), order.getOrderNumber(), tenantId, order.getPatientId(), order.getOrderType(), order.getCreatedAt()
        ));

        return toDto(order);
    }

    @Transactional(readOnly = true)
    public OpticalOrderDto getOrder(UUID tenantId, UUID orderId) {
        OpticalOrderEntity order = findOrderOrThrow(tenantId, orderId);
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public List<OpticalOrderDto> listOrdersByStage(UUID tenantId, OrderStage stage) {
        return orderRepository.findByTenantIdAndStage(tenantId, stage).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public OpticalOrderDto updateOrderStage(UUID tenantId, UUID orderId, UpdateStageRequest request) {
        OpticalOrderEntity order = findOrderOrThrow(tenantId, orderId);
        OrderStage oldStage = order.getStage();
        OrderStage newStage = request.getNewStage();

        if (oldStage == newStage) {
            return toDto(order);
        }

        validateStageTransition(oldStage, newStage, order);

        order.setStage(newStage);
        
        switch (newStage) {
            case RECEIVED -> {} // Initial stage, no side effects
            case SENT_TO_LAB -> order.setSentToLabAt(Instant.now());
            case QC_CHECK -> {} // Maybe set something
            case READY_FOR_FITTING -> {
                order.setReadyAt(Instant.now());
                eventPublisher.publishEvent(new OpticalOrderReadyEvent(order.getId(), order.getOrderNumber(), tenantId, order.getPatientId(), Instant.now()));
            }
            case COMPLETED -> order.setCompletedAt(Instant.now());
            case CANCELLED -> {
                order.setCancelledAt(Instant.now());
                order.setCancellationReason(request.getCancellationReason());
            }
        }

        order = orderRepository.save(order);

        eventPublisher.publishEvent(new OpticalOrderStatusChangedEvent(
                order.getId(), order.getOrderNumber(), tenantId, order.getPatientId(), oldStage, newStage, Instant.now()
        ));

        return toDto(order);
    }

    @Transactional
    public OpticalOrderDto addItem(UUID tenantId, UUID orderId, AddOrderItemRequest request) {
        OpticalOrderEntity order = findOrderOrThrow(tenantId, orderId);
        
        OpticalOrderItemEntity item = new OpticalOrderItemEntity();
        item.setOrderId(order.getId());
        item.setServiceItemId(request.serviceItemId());
        item.setStockItemId(request.stockItemId());
        item.setDescription(request.description());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setDiscountPercent(request.discountPercent() != null ? request.discountPercent() : java.math.BigDecimal.ZERO);
        item.setEye(request.eye());
        item.setNotes(request.notes());
        
        BigDecimal discount = request.discountPercent() != null ? request.discountPercent() : BigDecimal.ZERO;
        BigDecimal lineTotal = request.unitPrice()
                .multiply(new BigDecimal(request.quantity()))
                .multiply(BigDecimal.ONE.subtract(discount.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP)));
        item.setLineTotal(lineTotal);
        
        if (request.stockItemId() != null) {
            stockService.deductStock(tenantId, request.stockItemId(), request.quantity());
        }
        
        itemRepository.save(item);
        updateOrderTotal(order);
        
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OpticalOrderDto removeItem(UUID tenantId, UUID orderId, UUID itemId) {
        OpticalOrderEntity order = findOrderOrThrow(tenantId, orderId);
        OpticalOrderItemEntity item = itemRepository.findById(itemId)
                .filter(i -> i.getOrderId().equals(order.getId()))
                .orElseThrow(() -> new OpticalDomainException("ITEM_NOT_FOUND", "Item not found in order", HttpStatus.NOT_FOUND));
        
        itemRepository.delete(item);
        updateOrderTotal(order);
        
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OpticalOrderDto submitQcResult(UUID tenantId, UUID orderId, QcResultDto qcResult) {
        OpticalOrderEntity order = findOrderOrThrow(tenantId, orderId);
        try {
            order.setQcResult(objectMapper.writeValueAsString(qcResult));
            if (qcResult.isAllPassed()) {
                order.setQcPassedAt(Instant.now());
            }
        } catch (JacksonException e) {
            throw new OpticalDomainException("INVALID_QC_FORMAT", "Failed to serialize QC result", HttpStatus.BAD_REQUEST);
        }
        return toDto(orderRepository.save(order));
    }

    private void updateOrderTotal(OpticalOrderEntity order) {
        List<OpticalOrderItemEntity> items = itemRepository.findByOrderId(order.getId());
        order.setTotalAmount(totalCalculator.calculateTotal(items, order.getDepositPaid()));
    }

    private void validateStageTransition(OrderStage oldStage, OrderStage newStage, OpticalOrderEntity order) {
        if (newStage == OrderStage.CANCELLED) {
            if (oldStage == OrderStage.COMPLETED) {
                throw new OpticalDomainException("INVALID_TRANSITION", "Cannot cancel a completed order", org.springframework.http.HttpStatus.BAD_REQUEST);
            }
            return;
        }

        boolean valid = switch (oldStage) {
            case RECEIVED -> newStage == OrderStage.SENT_TO_LAB;
            case SENT_TO_LAB -> newStage == OrderStage.QC_CHECK;
            case QC_CHECK -> newStage == OrderStage.READY_FOR_FITTING || newStage == OrderStage.SENT_TO_LAB;
            case READY_FOR_FITTING -> newStage == OrderStage.COMPLETED;
            default -> false;
        };

        if (!valid) {
            throw new OpticalDomainException("INVALID_TRANSITION", "Invalid stage transition from " + oldStage + " to " + newStage, org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        if (newStage == OrderStage.READY_FOR_FITTING) {
            validateQcChecklist(order);
        }
    }

    private void validateQcChecklist(OpticalOrderEntity order) {
        if (order.getQcResult() == null || order.getQcResult().isBlank()) {
            throw new OpticalDomainException("QC_FAILED", "QC checklist must be completed before marking as ready", org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT);
        }
        QcResultDto qc;
        try {
            qc = objectMapper.readValue(order.getQcResult(), QcResultDto.class);
        } catch (JacksonException e) {
            throw new OpticalDomainException("QC_FAILED", "Invalid QC result format", org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT);
        }
        if (!qc.isAllPassed()) {
            throw new OpticalDomainException("QC_FAILED", "All QC checks must pass before marking as ready", org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    private OpticalOrderEntity findOrderOrThrow(UUID tenantId, UUID orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getTenantId().equals(tenantId))
                .orElseThrow(() -> new OpticalDomainException("ORDER_NOT_FOUND", "Order not found", org.springframework.http.HttpStatus.NOT_FOUND));
    }

    private OpticalOrderDto toDto(OpticalOrderEntity order) {
        List<OpticalOrderItemDto> items = itemRepository.findByOrderId(order.getId()).stream()
                .map(OpticalOrderItemDto::from)
                .toList();
        return OpticalOrderDto.from(order, items, objectMapper);
    }
}
