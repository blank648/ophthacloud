package ro.ophthacloud.modules.optical;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.ophthacloud.infrastructure.persistence.BaseEntity;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;
import ro.ophthacloud.modules.optical.dto.CreateOrderRequest;
import ro.ophthacloud.modules.optical.dto.QcResultDto;
import ro.ophthacloud.modules.optical.dto.UpdateStageRequest;
import ro.ophthacloud.modules.optical.event.OpticalOrderCreatedEvent;
import ro.ophthacloud.modules.optical.internal.*;
import ro.ophthacloud.shared.audit.AuditLogService;
import ro.ophthacloud.shared.security.ModulePermissions;
import ro.ophthacloud.shared.security.OphthaClinicalAuthenticationToken;
import ro.ophthacloud.shared.security.OphthaPrincipal;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OpticalFacade} backed by {@link OpticalOrderService}.
 * Pure Mockito — no Spring context, no DB.
 * Covers: create-order, stage transitions, QC gate, invalid transitions.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OpticalFacade — unit")
class OpticalFacadeTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────

    @Mock private OpticalOrderRepository       orderRepository;
    @Mock private OpticalOrderItemRepository   itemRepository;
    @Mock private OrderNumberGenerator         numberGenerator;
    @Mock private OrderTotalCalculator         totalCalculator;
    @Mock private StockService                 stockService;
    @Mock private ApplicationEventPublisher    eventPublisher;
    @Mock private AuditLogService              auditLogService;

    private final ObjectMapper objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();

    private OpticalOrderService orderService;

    @InjectMocks // injected into OpticalFacade below
    private OpticalFacade facade;

    private static final UUID TENANT_ID   = UUID.randomUUID();
    private static final UUID PATIENT_ID  = UUID.randomUUID();
    private static final UUID STAFF_ID    = UUID.randomUUID();
    private static final UUID ORDER_ID    = UUID.randomUUID();

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID);
        OphthaPrincipal principal = new OphthaPrincipal(
                STAFF_ID.toString(),
                TENANT_ID.toString(),
                STAFF_ID.toString(),
                null,
                "DOCTOR",
                Map.of("optical", new ModulePermissions(true, true, true, true, false, false))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new OphthaClinicalAuthenticationToken(principal, null, List.of()));

        // Construct service manually — ObjectMapper can't be @InjectMocks'd alongside @Mock
        orderService = new OpticalOrderService(
                orderRepository, itemRepository, numberGenerator,
                totalCalculator, stockService, eventPublisher, objectMapper,
                auditLogService
        );
        facade = new OpticalFacade(orderService);

        // Default stub: itemRepository returns empty list for any orderId
        when(itemRepository.findByOrderId(any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ── Test 1: createOrder succeeds ─────────────────────────────────────────

    @Test
    @DisplayName("createOrder: should save order with RECEIVED stage and publish OpticalOrderCreatedEvent")
    void createOrder_shouldSaveAndPublishEvent() {
        when(numberGenerator.generate(any(), any())).thenReturn("CMD-2026-000001");
        when(orderRepository.save(any())).thenAnswer(inv -> {
            OpticalOrderEntity e = inv.getArgument(0);
            setId(e, ORDER_ID);
            setTenantId(e, TENANT_ID);
            return e;
        });

        var result = facade.createOrder(TENANT_ID, validCreateRequest(), STAFF_ID);

        assertThat(result).isNotNull();
        assertThat(result.orderNumber()).isEqualTo("CMD-2026-000001");
        assertThat(result.stage()).isEqualTo(OrderStage.RECEIVED);

        verify(eventPublisher).publishEvent(any(OpticalOrderCreatedEvent.class));
    }

    // ── Test 2: getOrder — not found → 404 ───────────────────────────────────

    @Test
    @DisplayName("getOrder: should throw OpticalDomainException with ORDER_NOT_FOUND when id unknown")
    void getOrder_shouldThrowNotFound_whenUnknownId() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.getOrder(TENANT_ID, UUID.randomUUID()))
                .isInstanceOf(OpticalDomainException.class)
                .hasMessageContaining("Order not found");
    }

    // ── Test 3: Stage RECEIVED → SENT_TO_LAB (valid) ─────────────────────────

    @Test
    @DisplayName("updateOrderStage: RECEIVED→SENT_TO_LAB should succeed and stamp sentToLabAt")
    void updateOrderStage_receivedToSentToLab_shouldSucceed() {
        OpticalOrderEntity order = orderEntity(ORDER_ID, OrderStage.RECEIVED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        var result = facade.updateOrderStage(TENANT_ID, ORDER_ID, stageRequest(OrderStage.SENT_TO_LAB));

        assertThat(result.stage()).isEqualTo(OrderStage.SENT_TO_LAB);
        assertThat(order.getSentToLabAt()).isNotNull();
        verify(auditLogService).log(any());
    }

    // ── Test 4: Stage SENT_TO_LAB → QC_CHECK (valid) ─────────────────────────

    @Test
    @DisplayName("updateOrderStage: SENT_TO_LAB→QC_CHECK should succeed")
    void updateOrderStage_sentToLabToQcCheck_shouldSucceed() {
        OpticalOrderEntity order = orderEntity(ORDER_ID, OrderStage.SENT_TO_LAB);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        var result = facade.updateOrderStage(TENANT_ID, ORDER_ID, stageRequest(OrderStage.QC_CHECK));

        assertThat(result.stage()).isEqualTo(OrderStage.QC_CHECK);
    }

    // ── Test 5: QC gate — no QC result → 422 ─────────────────────────────────

    @Test
    @DisplayName("updateOrderStage: QC_CHECK→READY_FOR_FITTING without QC result should throw 422")
    void updateOrderStage_qcGate_noResult_shouldThrow422() {
        OpticalOrderEntity order = orderEntity(ORDER_ID, OrderStage.QC_CHECK);
        // qcResult is null
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> facade.updateOrderStage(
                TENANT_ID, ORDER_ID, stageRequest(OrderStage.READY_FOR_FITTING)))
                .isInstanceOf(OpticalDomainException.class)
                .hasMessageContaining("QC");
    }

    // ── Test 6: QC gate — partial QC (not all checks pass) → 422 ─────────────

    @Test
    @DisplayName("updateOrderStage: QC_CHECK→READY_FOR_FITTING with partial QC should throw 422")
    void updateOrderStage_qcGate_partialQc_shouldThrow422() throws Exception {
        OpticalOrderEntity order = orderEntity(ORDER_ID, OrderStage.QC_CHECK);
        // All 8 fields but one is false
        QcResultDto partialQc = new QcResultDto(
                true, true, true, true, true, true, true, false /* finalCleaning */
        );
        order.setQcResult(objectMapper.writeValueAsString(partialQc));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> facade.updateOrderStage(
                TENANT_ID, ORDER_ID, stageRequest(OrderStage.READY_FOR_FITTING)))
                .isInstanceOf(OpticalDomainException.class)
                .hasMessageContaining("QC");
    }

    // ── Test 7: QC gate — all 8 checks pass → advance succeeds ──────────────

    @Test
    @DisplayName("updateOrderStage: QC_CHECK→READY_FOR_FITTING with all QC passed should succeed")
    void updateOrderStage_qcGate_allPassed_shouldSucceed() throws Exception {
        OpticalOrderEntity order = orderEntity(ORDER_ID, OrderStage.QC_CHECK);
        QcResultDto fullQc = new QcResultDto(true, true, true, true, true, true, true, true);
        order.setQcResult(objectMapper.writeValueAsString(fullQc));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        var result = facade.updateOrderStage(TENANT_ID, ORDER_ID, stageRequest(OrderStage.READY_FOR_FITTING));

        assertThat(result.stage()).isEqualTo(OrderStage.READY_FOR_FITTING);
        assertThat(order.getReadyAt()).isNotNull();
    }

    // ── Test 8: Invalid transition RECEIVED → COMPLETED → 400 ────────────────

    @Test
    @DisplayName("updateOrderStage: RECEIVED→COMPLETED is invalid — should throw 400 INVALID_TRANSITION")
    void updateOrderStage_invalidTransition_shouldThrow400() {
        OpticalOrderEntity order = orderEntity(ORDER_ID, OrderStage.RECEIVED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> facade.updateOrderStage(
                TENANT_ID, ORDER_ID, stageRequest(OrderStage.COMPLETED)))
                .isInstanceOf(OpticalDomainException.class)
                .hasMessageContaining("Invalid stage transition");
    }

    // ── Test 9: Cancel from COMPLETED → 400 ──────────────────────────────────

    @Test
    @DisplayName("updateOrderStage: COMPLETED→CANCELLED should throw 400")
    void updateOrderStage_cancelCompleted_shouldThrow400() {
        OpticalOrderEntity order = orderEntity(ORDER_ID, OrderStage.COMPLETED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> facade.updateOrderStage(
                TENANT_ID, ORDER_ID, stageRequest(OrderStage.CANCELLED)))
                .isInstanceOf(OpticalDomainException.class);
    }

    // ── Test 10: listOrders delegates correctly ────────────────────────────────

    @Test
    @DisplayName("listOrders: should return orders for given stage")
    void listOrders_shouldReturnFilteredByStage() {
        OpticalOrderEntity order = orderEntity(ORDER_ID, OrderStage.RECEIVED);
        when(orderRepository.findByTenantIdAndStage(TENANT_ID, OrderStage.RECEIVED))
                .thenReturn(List.of(order));

        var result = facade.listOrders(TENANT_ID, OrderStage.RECEIVED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).stage()).isEqualTo(OrderStage.RECEIVED);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreateOrderRequest validCreateRequest() {
        return CreateOrderRequest.builder()
                .patientId(PATIENT_ID)
                .orderType(OrderType.GLASSES)
                .depositPaid(BigDecimal.ZERO)
                .build();
    }

    private UpdateStageRequest stageRequest(OrderStage stage) {
        return UpdateStageRequest.builder().newStage(stage).build();
    }

    private OpticalOrderEntity orderEntity(UUID id, OrderStage stage) {
        OpticalOrderEntity e = new OpticalOrderEntity();
        setId(e, id);
        setTenantId(e, TENANT_ID);
        e.setPatientId(PATIENT_ID);
        e.setOrderType(OrderType.GLASSES);
        e.setStage(stage);
        e.setOrderNumber("CMD-2026-000001");
        e.setDepositPaid(BigDecimal.ZERO);
        e.setTotalAmount(BigDecimal.ZERO);
        e.setCurrency("RON");
        return e;
    }

    private void setId(Object entity, UUID id) {
        setField(entity, BaseEntity.class, "id", id);
    }

    private void setTenantId(Object entity, UUID tenantId) {
        setField(entity, TenantAwareEntity.class, "tenantId", tenantId);
    }

    private void setField(Object target, Class<?> declaringClass, String name, Object value) {
        try {
            Field f = declaringClass.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field '" + name + "' via reflection", e);
        }
    }
}
