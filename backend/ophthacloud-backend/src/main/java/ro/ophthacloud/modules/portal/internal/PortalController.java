package ro.ophthacloud.modules.portal.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.portal.PortalFacade;
import ro.ophthacloud.modules.portal.dto.*;
import ro.ophthacloud.shared.api.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Patient Portal — GUIDE_04 §12.
 * <p>
 * All endpoints are read-only except {@link #updateConsents} (GDPR consents).
 * The patient's identity is always extracted from the JWT — never from path/query params.
 * Access is restricted to users with the {@code PATIENT} role.
 */
@RestController
@RequestMapping("/api/v1/portal/me")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Portal", description = "Patient-facing read-only API for own data")
@PreAuthorize("hasRole('PATIENT')")
public class PortalController {

    private final PortalFacade portalFacade;

    // ── Profile ──────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get my profile", description = "Returns the authenticated patient's own profile")
    public ApiResponse<PortalProfileDto> getMyProfile() {
        log.debug("Portal REST: GET /portal/me");
        return ApiResponse.of(portalFacade.getMyProfile());
    }

    // ── Appointments ─────────────────────────────────────────────────────────

    @GetMapping("/appointments")
    @Operation(summary = "Get my appointments", description = "Returns the authenticated patient's upcoming appointments")
    public ApiResponse<List<PortalAppointmentDto>> getMyAppointments() {
        log.debug("Portal REST: GET /portal/me/appointments");
        return ApiResponse.of(portalFacade.getMyAppointments());
    }

    // ── Prescriptions ────────────────────────────────────────────────────────

    @GetMapping("/prescriptions")
    @Operation(summary = "Get my prescriptions", description = "Returns the authenticated patient's active prescriptions")
    public ApiResponse<List<PortalPrescriptionSummaryDto>> getMyPrescriptions(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Portal REST: GET /portal/me/prescriptions");
        return ApiResponse.of(portalFacade.getMyPrescriptions(pageable));
    }

    @GetMapping("/prescriptions/{id}")
    @Operation(summary = "Get prescription detail", description = "Returns a specific prescription with line items and QR code")
    public ApiResponse<PortalPrescriptionDetailDto> getMyPrescriptionDetail(@PathVariable UUID id) {
        log.debug("Portal REST: GET /portal/me/prescriptions/{}", id);
        return ApiResponse.of(portalFacade.getMyPrescriptionDetail(id));
    }

    // ── Investigations ───────────────────────────────────────────────────────

    @GetMapping("/investigations")
    @Operation(summary = "Get my investigations", description = "Returns the authenticated patient's investigation results")
    public ApiResponse<List<PortalInvestigationDto>> getMyInvestigations(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Portal REST: GET /portal/me/investigations");
        return ApiResponse.of(portalFacade.getMyInvestigations(pageable));
    }

    // ── Optical Orders ───────────────────────────────────────────────────────

    @GetMapping("/optical-orders")
    @Operation(summary = "Get my optical orders", description = "Returns the authenticated patient's optical order status")
    public ApiResponse<List<PortalOpticalOrderDto>> getMyOpticalOrders() {
        log.debug("Portal REST: GET /portal/me/optical-orders");
        return ApiResponse.of(portalFacade.getMyOpticalOrders());
    }

    // ── Notifications ────────────────────────────────────────────────────────

    @GetMapping("/notifications")
    @Operation(summary = "Get my notifications", description = "Returns the authenticated patient's messages from clinic")
    public ApiResponse<List<PortalNotificationDto>> getMyNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Portal REST: GET /portal/me/notifications");
        return ApiResponse.of(portalFacade.getMyNotifications(pageable));
    }

    // ── Consents ─────────────────────────────────────────────────────────────

    @PutMapping("/consents")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update GDPR consents", description = "Updates the authenticated patient's GDPR consent preferences")
    public void updateConsents(@Valid @RequestBody UpdateConsentsRequest request) {
        log.debug("Portal REST: PUT /portal/me/consents");
        portalFacade.updateMyConsents(request);
    }
}
