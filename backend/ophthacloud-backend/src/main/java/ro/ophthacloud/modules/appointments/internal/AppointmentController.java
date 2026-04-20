package ro.ophthacloud.modules.appointments.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.appointments.AppointmentManagementFacade;
import ro.ophthacloud.modules.appointments.dto.*;
import ro.ophthacloud.shared.api.ApiResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for the Appointments module.
 * Implements GUIDE_04 §3.1 – §3.8.
 * Sits inside {@code internal/} — only the facade is public per GUIDE_07 §1.2.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Appointments", description = "Endpoints for managing appointments")
public class AppointmentController {

    private final AppointmentManagementFacade appointmentManagementFacade;

    // ── Appointments ─────────────────────────────────────────────────────────

    /**
     * GET /api/v1/appointments — non-paginated calendar feed within a date range.
     * Requires `from` and `to` query parameters (YYYY-MM-DD format).
     */
    @Operation(summary = "List appointments (calendar feed)", description = "Retrieves a non-paginated list of appointments for a specific date range. Max 31 days.")
    @GetMapping("/api/v1/appointments")
    @PreAuthorize("hasPermission('appointments', 'MODULE', 'VIEW')")
    public ApiResponse<List<AppointmentDto>> listCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) UUID patientId) {
        log.debug("REST listCalendar: from={}, to={}, doctorId={}, patientId={}", from, to, doctorId, patientId);
        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant   = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return ApiResponse.of(appointmentManagementFacade.listCalendar(fromInstant, toInstant, doctorId, patientId));
    }

    @Operation(summary = "Create an appointment")
    @PostMapping("/api/v1/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('appointments', 'MODULE', 'CREATE')")
    public ApiResponse<AppointmentDto> createAppointment(@Valid @RequestBody AppointmentRequest request) {
        log.debug("REST createAppointment: patient={}, doctor={}", request.getPatientId(), request.getDoctorId());
        return ApiResponse.of(appointmentManagementFacade.createAppointment(request));
    }

    @Operation(summary = "Get an appointment by ID")
    @GetMapping("/api/v1/appointments/{id}")
    @PreAuthorize("hasPermission('appointments', 'MODULE', 'VIEW')")
    public ApiResponse<AppointmentDto> getAppointment(@PathVariable UUID id) {
        return ApiResponse.of(appointmentManagementFacade.getAppointment(id));
    }

    @Operation(summary = "Update an appointment")
    @PutMapping("/api/v1/appointments/{id}")
    @PreAuthorize("hasPermission('appointments', 'MODULE', 'EDIT')")
    public ApiResponse<AppointmentDto> updateAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentRequest request) {
        return ApiResponse.of(appointmentManagementFacade.updateAppointment(id, request));
    }

    @Operation(summary = "Update appointment status")
    @PatchMapping("/api/v1/appointments/{id}/status")
    @PreAuthorize("hasPermission('appointments', 'MODULE', 'EDIT')")
    public ApiResponse<AppointmentDto> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        log.debug("REST updateStatus: id={}, newStatus={}", id, request.getStatus());
        return ApiResponse.of(appointmentManagementFacade.updateStatus(id, request));
    }

    @Operation(summary = "Cancel/delete an appointment")
    @DeleteMapping("/api/v1/appointments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission('appointments', 'MODULE', 'DELETE')")
    public void deleteAppointment(@PathVariable UUID id) {
        appointmentManagementFacade.deleteAppointment(id);
    }
}
