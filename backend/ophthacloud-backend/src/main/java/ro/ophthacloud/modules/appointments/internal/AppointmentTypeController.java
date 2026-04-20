package ro.ophthacloud.modules.appointments.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.appointments.AppointmentManagementFacade;
import ro.ophthacloud.modules.appointments.dto.AppointmentTypeDto;
import ro.ophthacloud.modules.appointments.dto.AppointmentTypeRequest;
import ro.ophthacloud.shared.api.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Appointment Types.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Appointment Types", description = "Endpoints for managing appointment types")
public class AppointmentTypeController {

    private final AppointmentManagementFacade appointmentManagementFacade;

    @Operation(summary = "List all active appointment types")
    @GetMapping("/api/v1/appointment-types")
    @PreAuthorize("hasPermission('appointments', 'MODULE', 'VIEW')")
    public ApiResponse<List<AppointmentTypeDto>> listTypes() {
        return ApiResponse.of(appointmentManagementFacade.listTypes());
    }

    @Operation(summary = "Create a new appointment type")
    @PostMapping("/api/v1/appointment-types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('appointments', 'MODULE', 'CREATE')")
    public ApiResponse<AppointmentTypeDto> createType(@Valid @RequestBody AppointmentTypeRequest request) {
        return ApiResponse.of(appointmentManagementFacade.createType(request));
    }

    @Operation(summary = "Update an existing appointment type")
    @PutMapping("/api/v1/appointment-types/{id}")
    @PreAuthorize("hasPermission('appointments', 'MODULE', 'EDIT')")
    public ApiResponse<AppointmentTypeDto> updateType(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentTypeRequest request) {
        return ApiResponse.of(appointmentManagementFacade.updateType(id, request));
    }
}
