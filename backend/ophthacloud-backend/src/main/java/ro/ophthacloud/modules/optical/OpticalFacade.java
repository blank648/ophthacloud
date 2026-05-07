package ro.ophthacloud.modules.optical;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.ophthacloud.modules.optical.dto.CreateOrderRequest;
import ro.ophthacloud.modules.optical.dto.OpticalOrderDto;
import ro.ophthacloud.modules.optical.dto.UpdateStageRequest;
import ro.ophthacloud.modules.optical.internal.OpticalOrderService;
import ro.ophthacloud.modules.optical.internal.OrderStage;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpticalFacade {

    private final OpticalOrderService orderService;

    public OpticalOrderDto createOrder(UUID tenantId, CreateOrderRequest request, UUID createdById) {
        return orderService.createOrder(tenantId, request, createdById);
    }

    public OpticalOrderDto getOrder(UUID tenantId, UUID orderId) {
        return orderService.getOrder(tenantId, orderId);
    }

    public List<OpticalOrderDto> listOrders(UUID tenantId, OrderStage stage) {
        return orderService.listOrdersByStage(tenantId, stage);
    }

    public OpticalOrderDto updateOrderStage(UUID tenantId, UUID orderId, UpdateStageRequest request) {
        return orderService.updateOrderStage(tenantId, orderId, request);
    }
}
