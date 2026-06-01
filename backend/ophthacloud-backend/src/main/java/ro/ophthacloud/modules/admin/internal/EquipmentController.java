package ro.ophthacloud.modules.admin.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.admin.dto.CreateEquipmentRequest;
import ro.ophthacloud.modules.admin.dto.EquipmentDto;
import ro.ophthacloud.shared.api.ApiResponse;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/equipment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin / Equipment", description = "Endpoints for managing clinic equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    @PreAuthorize("hasPermission('admin', 'MODULE', 'VIEW')")
    @Operation(summary = "List all equipment")
    public ApiResponse<List<EquipmentDto>> listEquipment() {
        log.debug("REST request to list equipment");
        List<EquipmentDto> equipment = equipmentService.listEquipment();
        return ApiResponse.of(equipment);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('admin', 'MODULE', 'CREATE')")
    @Operation(summary = "Create equipment")
    public ApiResponse<EquipmentDto> createEquipment(@Valid @RequestBody CreateEquipmentRequest request) {
        log.debug("REST request to create equipment: {}", request.name());
        EquipmentDto equipment = equipmentService.createEquipment(request);
        return ApiResponse.of(equipment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('admin', 'MODULE', 'DELETE')")
    @Operation(summary = "Delete equipment")
    public ApiResponse<Void> deleteEquipment(@PathVariable UUID id) {
        log.debug("REST request to delete equipment: {}", id);
        equipmentService.deleteEquipment(id);
        return ApiResponse.of(null);
    }
}
