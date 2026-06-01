package ro.ophthacloud.modules.optical.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import ro.ophthacloud.modules.optical.dto.InvoiceDto;
import ro.ophthacloud.modules.optical.dto.InvoiceLineRequest;
import ro.ophthacloud.modules.optical.dto.PayInvoiceRequest;
import ro.ophthacloud.shared.api.PdfDownloadResponse;
import ro.ophthacloud.shared.util.DocumentStorageService;
import ro.ophthacloud.shared.util.PdfGenerationService;

import java.math.BigDecimal;
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
    private final OpticalOrderRepository opticalOrderRepository;
    private final OpticalOrderItemRepository opticalOrderItemRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PdfGenerationService pdfGenerationService;
    private final DocumentStorageService documentStorageService;

    public String fetchPatientName(UUID patientId) {
        if (patientId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT first_name || ' ' || last_name FROM patients WHERE id = ?",
                    String.class, patientId);
        } catch (Exception e) {
            log.warn("Could not fetch patient name: patientId={}", patientId);
            return null;
        }
    }

    @Transactional
    public InvoiceDto createInvoice(UUID tenantId, UUID opticalOrderId, UUID patientId, UUID createdById) {
        return createInvoice(tenantId, opticalOrderId, patientId, createdById, null);
    }

    @Transactional
    public InvoiceDto createInvoice(UUID tenantId, UUID opticalOrderId, UUID patientId, UUID createdById, List<InvoiceLineRequest> items) {
        // Verify patient belongs to tenant
        Boolean patientExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM patients WHERE id = ? AND tenant_id = ?)",
                Boolean.class, patientId, tenantId);
        if (!Boolean.TRUE.equals(patientExists)) {
            throw new OpticalDomainException("PATIENT_NOT_FOUND", "Patient does not exist in this tenant scope", HttpStatus.NOT_FOUND);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal vatTotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        if (opticalOrderId != null) {
            OpticalOrderEntity order = opticalOrderRepository.findById(opticalOrderId)
                    .filter(o -> o.getTenantId().equals(tenantId))
                    .orElseThrow(() -> new OpticalDomainException("ORDER_NOT_FOUND", "Order does not exist in this tenant scope", HttpStatus.NOT_FOUND));
            
            total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal vatMultiplier = new BigDecimal("1.19");
            subtotal = total.divide(vatMultiplier, 4, java.math.RoundingMode.HALF_UP);
            vatTotal = total.subtract(subtotal);
        } else if (items != null && !items.isEmpty()) {
            for (InvoiceLineRequest item : items) {
                BigDecimal qty = BigDecimal.valueOf(item.quantity());
                BigDecimal grossPrice = item.unitPrice();
                BigDecimal lineGrossTotal = grossPrice.multiply(qty);
                total = total.add(lineGrossTotal);

                BigDecimal vatRate = item.vatRate() != null ? item.vatRate() : new BigDecimal("19.00");
                BigDecimal vatFactor = BigDecimal.ONE.add(vatRate.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
                BigDecimal lineNetTotal = lineGrossTotal.divide(vatFactor, 4, java.math.RoundingMode.HALF_UP);

                subtotal = subtotal.add(lineNetTotal);
            }
            vatTotal = total.subtract(subtotal);
        }

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setOpticalOrderId(opticalOrderId);
        invoice.setPatientId(patientId);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setCreatedById(createdById);
        invoice.setSubtotal(subtotal);
        invoice.setVatTotal(vatTotal);
        invoice.setTotal(total);

        invoice.setInvoiceNumber(invoiceNumberGenerator.generate(tenantId, Instant.now()));

        invoice = invoiceRepository.save(invoice);

        if (opticalOrderId != null) {
            List<OpticalOrderItemEntity> orderItems = opticalOrderItemRepository.findByOrderId(opticalOrderId);
            for (OpticalOrderItemEntity item : orderItems) {
                InvoiceLineEntity line = new InvoiceLineEntity();
                line.setInvoiceId(invoice.getId());
                line.setDescription(item.getDescription());
                line.setQuantity(item.getQuantity());
                
                BigDecimal grossPrice = item.getUnitPrice();
                BigDecimal netPrice = grossPrice.divide(new BigDecimal("1.19"), 4, java.math.RoundingMode.HALF_UP);
                line.setUnitPrice(netPrice);
                line.setVatRate(new BigDecimal("19.00"));
                line.setDiscountPercent(item.getDiscountPercent() != null ? item.getDiscountPercent() : BigDecimal.ZERO);
                
                BigDecimal netLineTotal = item.getLineTotal().divide(new BigDecimal("1.19"), 4, java.math.RoundingMode.HALF_UP);
                line.setLineTotal(netLineTotal);
                line.setServiceItemId(item.getServiceItemId());
                
                invoiceLineRepository.save(line);
            }
        } else if (items != null && !items.isEmpty()) {
            for (InvoiceLineRequest item : items) {
                InvoiceLineEntity line = new InvoiceLineEntity();
                line.setInvoiceId(invoice.getId());
                line.setDescription(item.description());
                line.setQuantity(item.quantity());
                
                BigDecimal vatRate = item.vatRate() != null ? item.vatRate() : new BigDecimal("19.00");
                BigDecimal vatFactor = BigDecimal.ONE.add(vatRate.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
                BigDecimal netPrice = item.unitPrice().divide(vatFactor, 4, java.math.RoundingMode.HALF_UP);
                BigDecimal lineGrossTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
                BigDecimal lineNetTotal = lineGrossTotal.divide(vatFactor, 4, java.math.RoundingMode.HALF_UP);

                line.setUnitPrice(netPrice);
                line.setVatRate(vatRate);
                line.setDiscountPercent(BigDecimal.ZERO);
                line.setLineTotal(lineNetTotal);
                line.setServiceItemId(item.serviceItemId());
                
                invoiceLineRepository.save(line);
            }
        }

        String patientName = fetchPatientName(patientId);
        List<InvoiceLineEntity> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
        return InvoiceDto.from(invoice, lines, patientName);
    }

    @Transactional(readOnly = true)
    public InvoiceDto getInvoice(UUID tenantId, UUID invoiceId) {
        InvoiceEntity entity = findInvoiceOrThrow(tenantId, invoiceId);
        String patientName = fetchPatientName(entity.getPatientId());
        List<InvoiceLineEntity> lines = invoiceLineRepository.findByInvoiceId(invoiceId);
        return InvoiceDto.from(entity, lines, patientName);
    }

    @Transactional
    public InvoiceDto payInvoice(UUID tenantId, UUID invoiceId, PayInvoiceRequest request) {
        InvoiceEntity invoice = findInvoiceOrThrow(tenantId, invoiceId);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentMethod(request.paymentMethod());
        invoice.setPaymentReference(request.paymentReference());
        invoice.setPaidAt(Instant.now());
        invoice.setAmountPaid(invoice.getTotal()); // Mark as fully paid for now
        
        invoice = invoiceRepository.save(invoice);
        String patientName = fetchPatientName(invoice.getPatientId());
        List<InvoiceLineEntity> lines = invoiceLineRepository.findByInvoiceId(invoiceId);
        return InvoiceDto.from(invoice, lines, patientName);
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
