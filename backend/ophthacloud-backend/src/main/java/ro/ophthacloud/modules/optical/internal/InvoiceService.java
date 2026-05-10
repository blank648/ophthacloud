package ro.ophthacloud.modules.optical.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import ro.ophthacloud.modules.optical.dto.InvoiceDto;
import ro.ophthacloud.modules.optical.dto.PayInvoiceRequest;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;

    @Transactional
    public InvoiceDto createInvoice(UUID tenantId, UUID opticalOrderId, UUID patientId, UUID createdById) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setOpticalOrderId(opticalOrderId);
        invoice.setPatientId(patientId);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setCreatedById(createdById);

        invoice.setInvoiceNumber(invoiceNumberGenerator.generate(tenantId, Instant.now()));

        invoice = invoiceRepository.save(invoice);
        return InvoiceDto.from(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDto getInvoice(UUID tenantId, UUID invoiceId) {
        return InvoiceDto.from(findInvoiceOrThrow(tenantId, invoiceId));
    }

    @Transactional
    public InvoiceDto payInvoice(UUID tenantId, UUID invoiceId, PayInvoiceRequest request) {
        InvoiceEntity invoice = findInvoiceOrThrow(tenantId, invoiceId);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentMethod(request.paymentMethod());
        invoice.setPaymentReference(request.paymentReference());
        invoice.setPaidAt(Instant.now());
        invoice.setAmountPaid(invoice.getTotal()); // Mark as fully paid for now
        
        return InvoiceDto.from(invoiceRepository.save(invoice));
    }

    private InvoiceEntity findInvoiceOrThrow(UUID tenantId, UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .filter(i -> i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new OpticalDomainException("INVOICE_NOT_FOUND", "Invoice not found", HttpStatus.NOT_FOUND));
    }
}
