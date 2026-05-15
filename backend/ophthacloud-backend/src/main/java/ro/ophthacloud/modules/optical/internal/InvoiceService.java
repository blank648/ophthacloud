package ro.ophthacloud.modules.optical.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import ro.ophthacloud.modules.optical.dto.InvoiceDto;
import ro.ophthacloud.modules.optical.dto.PayInvoiceRequest;
import ro.ophthacloud.shared.api.PdfDownloadResponse;
import ro.ophthacloud.shared.util.DocumentStorageService;
import ro.ophthacloud.shared.util.PdfGenerationService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final String MINIO_BUCKET = "generated-documents";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final JdbcTemplate jdbcTemplate;
    private final PdfGenerationService pdfGenerationService;
    private final DocumentStorageService documentStorageService;

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

    // ── PDF Generation ──────────────────────────────────────────────────────

    /**
     * Generates (or retrieves cached) invoice PDF.
     * Cache-on-generate: stores pdfPath on entity after first generation.
     * Returns presigned MinIO URL with 1-hour expiry.
     */
    @Transactional
    public PdfDownloadResponse generatePdf(UUID tenantId, UUID invoiceId) {
        InvoiceEntity invoice = findInvoiceOrThrow(tenantId, invoiceId);

        // Cache-on-generate: if already generated, return presigned URL
        if (invoice.getPdfPath() != null && !invoice.getPdfPath().isBlank()) {
            String url = documentStorageService.generatePresignedUrl(MINIO_BUCKET, invoice.getPdfPath(), 1);
            return new PdfDownloadResponse(url, Instant.now().plusSeconds(3600));
        }

        // Fetch patient name via JDBC (cross-module)
        String patientName = null;
        try {
            patientName = jdbcTemplate.queryForObject(
                    "SELECT first_name || ' ' || last_name FROM patients WHERE id = ?",
                    String.class, invoice.getPatientId());
        } catch (Exception e) {
            log.warn("Could not fetch patient name for invoice PDF: patientId={}", invoice.getPatientId());
        }

        // Fetch clinic name
        String clinicName = null;
        try {
            clinicName = jdbcTemplate.queryForObject(
                    "SELECT name FROM tenants WHERE id = ?",
                    String.class, tenantId);
        } catch (Exception e) {
            log.warn("Could not fetch clinic name for invoice PDF: tenantId={}", tenantId);
        }

        // Fetch invoice lines
        List<InvoiceLineEntity> lineEntities = invoiceLineRepository.findByInvoiceId(invoiceId);

        List<PdfGenerationService.InvoiceLine> pdfLines = lineEntities.stream()
                .map(line -> new PdfGenerationService.InvoiceLine(
                        line.getDescription(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getDiscountPercent(),
                        line.getVatRate(),
                        line.getLineTotal()
                ))
                .toList();

        // Generate PDF
        byte[] pdfBytes = pdfGenerationService.generateInvoicePdf(
                clinicName,
                invoice.getInvoiceNumber(),
                patientName,
                invoice.getIssuedAt(),
                invoice.getStatus().name(),
                invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().name() : null,
                invoice.getCurrency(),
                invoice.getSubtotal(),
                invoice.getVatTotal(),
                invoice.getDiscountTotal(),
                invoice.getTotal(),
                invoice.getPaidAt(),
                pdfLines
        );

        // Upload to MinIO
        String objectPath = tenantId + "/invoices/" + invoiceId + ".pdf";
        documentStorageService.upload(MINIO_BUCKET, objectPath, pdfBytes, "application/pdf");

        // Cache the path on entity
        invoice.setPdfPath(objectPath);
        invoiceRepository.save(invoice);

        String url = documentStorageService.generatePresignedUrl(MINIO_BUCKET, objectPath, 1);
        log.info("generatePdf: invoice={}, size={} bytes", invoiceId, pdfBytes.length);
        return new PdfDownloadResponse(url, Instant.now().plusSeconds(3600));
    }
}
