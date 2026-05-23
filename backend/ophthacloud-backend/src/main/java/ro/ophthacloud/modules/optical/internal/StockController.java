package ro.ophthacloud.modules.optical.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.optical.dto.StockItemDto;
import ro.ophthacloud.modules.optical.dto.UpdateStockRequest;
import ro.ophthacloud.shared.tenant.TenantContext;
import ro.ophthacloud.shared.api.ApiResponse;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/optical/stock")
@RequiredArgsConstructor
@Tag(name = "Optical / Stock", description = "Endpoints for managing optical inventory and stock")
public class StockController {

    private final StockService stockService;

    @GetMapping
    @PreAuthorize("hasPermission('optical', 'MODULE', 'VIEW')")
    @Operation(summary = "List all stock items")
    public ApiResponse<List<StockItemDto>> listStock() {
        return ApiResponse.of(stockService.listItems(TenantContext.require()));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasPermission('optical', 'MODULE', 'VIEW')")
    @Operation(summary = "Get low stock report")
    public ApiResponse<List<StockItemDto>> getLowStockReport() {
        return ApiResponse.of(stockService.getLowStockReport(TenantContext.require()));
    }

    @PatchMapping("/{id}/level")
    @PreAuthorize("hasPermission('optical', 'MODULE', 'EDIT')")
    @Operation(summary = "Update stock level for an item")
    public ApiResponse<StockItemDto> updateStockLevel(@PathVariable UUID id, @RequestBody @Valid UpdateStockRequest request) {
        return ApiResponse.of(stockService.updateStockLevel(TenantContext.require(), id, request.newStock()));
    }
}
