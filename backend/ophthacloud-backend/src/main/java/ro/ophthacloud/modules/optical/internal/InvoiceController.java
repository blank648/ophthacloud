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
import ro.ophthacloud.shared.security.SecurityUtils;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/optical/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('optical', 'MODULE', 'CREATE')")
    public ApiResponse<InvoiceDto> createInvoice(@RequestBody @Valid CreateInvoiceRequest request) {
        return ApiResponse.of(invoiceService.createInvoice(
                TenantContext.require(),
                request.opticalOrderId(),
                request.patientId(),
                UUID.fromString(SecurityUtils.currentStaffId())
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('optical', 'MODULE', 'VIEW')")
    public ApiResponse<InvoiceDto> getInvoice(@PathVariable UUID id) {
        return ApiResponse.of(invoiceService.getInvoice(TenantContext.require(), id));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasPermission('optical', 'MODULE', 'EDIT')")
    public ApiResponse<InvoiceDto> payInvoice(@PathVariable UUID id, @RequestBody @Valid PayInvoiceRequest request) {
        return ApiResponse.of(invoiceService.payInvoice(TenantContext.require(), id, request));
    }
}
