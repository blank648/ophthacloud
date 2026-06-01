package ro.ophthacloud.modules.optical.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.optical.dto.CreateInvoiceRequest;
import ro.ophthacloud.modules.optical.dto.InvoiceDto;
import ro.ophthacloud.modules.optical.dto.PayInvoiceRequest;
import ro.ophthacloud.shared.tenant.TenantContext;
import ro.ophthacloud.shared.api.ApiResponse;
import ro.ophthacloud.shared.api.PdfDownloadResponse;
import ro.ophthacloud.shared.security.SecurityUtils;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/optical/invoices")
@RequiredArgsConstructor
@Tag(name = "Optical / Invoices", description = "Endpoints for managing optical shop invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('optical', 'MODULE', 'CREATE')")
    @Operation(summary = "Create an invoice")
    public ApiResponse<InvoiceDto> createInvoice(@RequestBody @Valid CreateInvoiceRequest request) {
        return ApiResponse.of(invoiceService.createInvoice(
                TenantContext.require(),
                request.opticalOrderId(),
                request.patientId(),
                UUID.fromString(SecurityUtils.currentStaffId()),
                request.items()
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CLINIC_ADMIN', 'DOCTOR', 'RECEPTIONIST', 'OPTOMETRIST', 'NURSE', 'OPTICAL_TECHNICIAN', 'MANAGER')")
    @Operation(summary = "List all invoices for the tenant")
    public ApiResponse<List<InvoiceDto>> listInvoices(@RequestParam(required = false) InvoiceStatus status) {
        UUID tenantId = TenantContext.require();
        List<InvoiceEntity> entities = status != null
                ? invoiceRepository.findByTenantIdAndStatus(tenantId, status)
                : invoiceRepository.findAll().stream().filter(i -> i.getTenantId().equals(tenantId)).toList();

        return ApiResponse.of(entities.stream()
                .map(entity -> {
                    String patientName = invoiceService.fetchPatientName(entity.getPatientId());
                    List<InvoiceLineEntity> lines = invoiceLineRepository.findByInvoiceId(entity.getId());
                    return InvoiceDto.from(entity, lines, patientName);
                })
                .toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('optical', 'MODULE', 'VIEW')")
    @Operation(summary = "Get invoice by ID")
    public ApiResponse<InvoiceDto> getInvoice(@PathVariable UUID id) {
        return ApiResponse.of(invoiceService.getInvoice(TenantContext.require(), id));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasPermission('optical', 'MODULE', 'EDIT')")
    @Operation(summary = "Record payment for an invoice")
    public ApiResponse<InvoiceDto> payInvoice(@PathVariable UUID id, @RequestBody @Valid PayInvoiceRequest request) {
        return ApiResponse.of(invoiceService.payInvoice(TenantContext.require(), id, request));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasPermission('optical', 'MODULE', 'VIEW')")
    @Operation(summary = "Download invoice as PDF")
    public ApiResponse<PdfDownloadResponse> getInvoicePdf(@PathVariable UUID id) {
        return ApiResponse.of(invoiceService.generatePdf(TenantContext.require(), id));
    }
}
