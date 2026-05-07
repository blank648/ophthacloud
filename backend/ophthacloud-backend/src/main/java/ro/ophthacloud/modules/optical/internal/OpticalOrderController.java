package ro.ophthacloud.modules.optical.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.optical.dto.*;
import ro.ophthacloud.shared.tenant.TenantContext;
import ro.ophthacloud.shared.api.ApiResponse;
import ro.ophthacloud.shared.security.SecurityUtils;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/optical/orders")
@RequiredArgsConstructor
public class OpticalOrderController {

    private final OpticalOrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('optical', 'MODULE', 'CREATE')")
    public ApiResponse<OpticalOrderDto> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ApiResponse.of(orderService.createOrder(
                TenantContext.require(), 
                request, 
                UUID.fromString(SecurityUtils.currentStaffId())
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('optical', 'MODULE', 'VIEW')")
    public ApiResponse<OpticalOrderDto> getOrder(@PathVariable UUID id) {
        return ApiResponse.of(orderService.getOrder(TenantContext.require(), id));
    }

    @GetMapping
    @PreAuthorize("hasPermission('optical', 'MODULE', 'VIEW')")
    public ApiResponse<List<OpticalOrderDto>> listOrders(@RequestParam(required = false) OrderStage stage) {
        return ApiResponse.of(orderService.listOrdersByStage(TenantContext.require(), stage));
    }

    @PatchMapping("/{id}/stage")
    @PreAuthorize("hasPermission('optical', 'MODULE', 'EDIT')")
    public ApiResponse<OpticalOrderDto> updateStage(@PathVariable UUID id, @RequestBody @Valid UpdateStageRequest request) {
        return ApiResponse.of(orderService.updateOrderStage(TenantContext.require(), id, request));
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('optical', 'MODULE', 'EDIT')")
    public ApiResponse<OpticalOrderDto> addItem(@PathVariable UUID id, @RequestBody @Valid AddOrderItemRequest request) {
        return ApiResponse.of(orderService.addItem(TenantContext.require(), id, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasPermission('optical', 'MODULE', 'EDIT')")
    public ApiResponse<OpticalOrderDto> removeItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        return ApiResponse.of(orderService.removeItem(TenantContext.require(), id, itemId));
    }

    @PutMapping("/{id}/qc")
    @PreAuthorize("hasPermission('optical', 'MODULE', 'EDIT')")
    public ApiResponse<OpticalOrderDto> submitQc(@PathVariable UUID id, @RequestBody @Valid QcResultDto qcResult) {
        return ApiResponse.of(orderService.submitQcResult(TenantContext.require(), id, qcResult));
    }
}
