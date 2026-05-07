package ro.ophthacloud.modules.optical;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.ophthacloud.modules.optical.dto.InvoiceDto;
import ro.ophthacloud.modules.optical.internal.InvoiceService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceFacade {

    private final InvoiceService invoiceService;

    public InvoiceDto createInvoice(UUID tenantId, UUID opticalOrderId, UUID patientId, UUID createdById) {
        return invoiceService.createInvoice(tenantId, opticalOrderId, patientId, createdById);
    }
}
