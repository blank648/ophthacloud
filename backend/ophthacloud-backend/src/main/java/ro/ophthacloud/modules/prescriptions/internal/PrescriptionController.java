package ro.ophthacloud.modules.prescriptions.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.prescriptions.PrescriptionsFacade;
import ro.ophthacloud.modules.prescriptions.dto.CreatePrescriptionRequest;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionDto;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionVerifyDto;
import ro.ophthacloud.modules.prescriptions.enums.PrescriptionStatusType;
import ro.ophthacloud.shared.api.ApiResponse;
import ro.ophthacloud.shared.api.PagedApiResponse;
import ro.ophthacloud.shared.api.PdfDownloadResponse;
import ro.ophthacloud.shared.security.SecurityUtils;

import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Endpoints for managing medical prescriptions")
public class PrescriptionController {

    private final PrescriptionsFacade facade;
    private final JdbcTemplate jdbcTemplate;

    // ── Authenticated endpoints ───────────────────────────────────────────────

    @GetMapping("/api/v1/prescriptions")
    @PreAuthorize("hasPermission('prescriptions', 'MODULE', 'VIEW') or hasPermission('prescriptions', 'MODULE', 'SIGN')")
    @Operation(summary = "List prescriptions for a patient")
    public PagedApiResponse<PrescriptionDto> listPrescriptions(
            @RequestParam UUID patientId,
            @RequestParam(required = false) PrescriptionStatusType status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issuedAt"));
        Page<PrescriptionDto> result = facade.listPrescriptions(tenantId, patientId, status, pageable);
        return PagedApiResponse.of(result);
    }

    @PostMapping("/api/v1/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('prescriptions', 'MODULE', 'CREATE')")
    @Operation(summary = "Create a draft prescription")
    public ApiResponse<PrescriptionDto> createPrescription(@Valid @RequestBody CreatePrescriptionRequest request) {
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        ro.ophthacloud.shared.security.OphthaPrincipal principal = SecurityUtils.currentPrincipal();
        UUID issuedById = UUID.fromString(principal.staffId() != null ? principal.staffId() : principal.keycloakUserId());
        
        String issuedByName = "Dr. " + principal.staffRole();
        try {
            String realName = jdbcTemplate.queryForObject(
                "SELECT first_name || ' ' || last_name FROM staff_members WHERE tenant_id = ? AND (id = ? OR keycloak_user_id = ?)",
                String.class,
                tenantId,
                issuedById,
                principal.keycloakUserId() != null ? UUID.fromString(principal.keycloakUserId()) : issuedById
            );
            if (realName != null && !realName.isBlank()) {
                issuedByName = "Dr. " + realName;
            }
        } catch (Exception ignored) {}

        PrescriptionDto created = facade.createPrescription(tenantId, issuedById, issuedByName, request);
        return ApiResponse.of(created);
    }

    @GetMapping("/api/v1/prescriptions/{id}")
    @PreAuthorize("hasPermission('prescriptions', 'MODULE', 'VIEW') or hasPermission('prescriptions', 'MODULE', 'SIGN')")
    @Operation(summary = "Get prescription by ID")
    public ApiResponse<PrescriptionDto> getPrescription(@PathVariable UUID id) {
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        return ApiResponse.of(facade.getPrescription(tenantId, id));
    }

    @PostMapping("/api/v1/prescriptions/{id}/sign")
    @PreAuthorize("hasPermission('prescriptions', 'MODULE', 'SIGN')")
    @Operation(summary = "Sign a draft prescription")
    public ApiResponse<PrescriptionDto> signPrescription(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {

        Boolean confirmed = body.get("signatureConfirmation");
        if (!Boolean.TRUE.equals(confirmed)) {
            throw new IllegalArgumentException("signatureConfirmation must be true.");
        }
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        return ApiResponse.of(facade.signPrescription(tenantId, id));
    }

    @PostMapping("/api/v1/prescriptions/{id}/cancel")
    @PreAuthorize("hasPermission('prescriptions', 'MODULE', 'EDIT')")
    @Operation(summary = "Cancel an active prescription")
    public ApiResponse<PrescriptionDto> cancelPrescription(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {

        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        return ApiResponse.of(facade.cancelPrescription(tenantId, id));
    }

    // ── PDF Download ──────────────────────────────────────────────────────

    @GetMapping("/api/v1/prescriptions/{id}/pdf")
    @PreAuthorize("hasPermission('prescriptions', 'MODULE', 'VIEW')")
    @Operation(summary = "Download prescription as PDF")
    public ApiResponse<PdfDownloadResponse> getPrescriptionPdf(@PathVariable UUID id) {
        UUID tenantId = UUID.fromString(SecurityUtils.currentTenantId());
        return ApiResponse.of(facade.generatePdf(tenantId, id));
    }

    // ── Public QR Verification (no auth) ────────────────────────────────────

    @GetMapping("/api/v1/public/prescriptions/verify/{qrToken}")
    @Operation(summary = "Publicly verify a prescription by QR token")
    public ApiResponse<PrescriptionVerifyDto> verifyPrescription(@PathVariable UUID qrToken) {
        return ApiResponse.of(facade.verifyByQrToken(qrToken));
    }
}
