package ro.ophthacloud.shared.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates PDF documents for prescriptions and invoices.
 * <p>
 * Prescription PDF per GUIDE_04 §6.6 / GUIDE_06 §5.5:
 * - Clinic header, doctor info, patient demographics
 * - OD/OS refraction table, QR code, validity dates
 * <p>
 * Invoice PDF per GUIDE_04 §7.7:
 * - Romanian fiscal header, itemized lines, VAT breakdown
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private static final String QR_VERIFY_BASE_URL = "https://app.ophthacloud.ro/verify/";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");

    // Fonts
    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK);
    private static final Font FONT_SUBTITLE = new Font(Font.HELVETICA, 12, Font.BOLD, Color.DARK_GRAY);
    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
    private static final Font FONT_BODY = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Font FONT_SMALL = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Font FONT_MONO = new Font(Font.COURIER, 10, Font.NORMAL, Color.BLACK);

    private static final Color HEADER_BG = new Color(41, 98, 255);      // Blue header
    private static final Color TABLE_HEADER_BG = new Color(230, 236, 255); // Light blue
    private static final Color OD_COLOR = new Color(220, 53, 69);       // Red for OD
    private static final Color OS_COLOR = new Color(0, 123, 255);       // Blue for OS

    private final QrCodeGenerator qrCodeGenerator;

    // ── Prescription PDF ────────────────────────────────────────────────────

    /**
     * Generates a prescription PDF document.
     *
     * @param clinicName       clinic name from tenants table
     * @param doctorName       issuing doctor name
     * @param patientName      patient full name
     * @param patientDob       patient date of birth (as string, e.g. "1963-08-15")
     * @param patientMrn       patient MRN (e.g. "OC-000001")
     * @param prescriptionNumber prescription number (e.g. "RX-2026-004821")
     * @param prescriptionType  e.g. "DISTANCE", "NEAR", "PROGRESSIVE"
     * @param validFrom         validity start date
     * @param validUntil        validity end date
     * @param pdBinocular       binocular PD
     * @param pdOd              monocular PD OD
     * @param pdOs              monocular PD OS
     * @param lensType          e.g. "SINGLE_VISION"
     * @param lensMaterial      e.g. "Poliuretan 1.67"
     * @param lensCoating       e.g. "Antireflectantă"
     * @param clinicalNotes     clinical notes
     * @param patientInstructions patient instructions
     * @param qrCodeToken       QR code token UUID string
     * @param lines             prescription lines (OD/OS refraction data)
     * @return PDF bytes
     */
    public byte[] generatePrescriptionPdf(
            String clinicName,
            String doctorName,
            String patientName,
            String patientDob,
            String patientMrn,
            String prescriptionNumber,
            String prescriptionType,
            LocalDate validFrom,
            LocalDate validUntil,
            BigDecimal pdBinocular,
            BigDecimal pdOd,
            BigDecimal pdOs,
            String lensType,
            String lensMaterial,
            String lensCoating,
            String clinicalNotes,
            String patientInstructions,
            String qrCodeToken,
            List<RefractionLine> lines) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            // ── Header ──────────────────────────────────────────────────
            addClinicHeader(document, clinicName);
            document.add(new Paragraph("REȚETĂ OFTALMOLOGICĂ", FONT_TITLE));
            document.add(Chunk.NEWLINE);

            // ── Prescription info ───────────────────────────────────────
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1, 1});

            addInfoRow(infoTable, "Nr. prescripție:", prescriptionNumber);
            addInfoRow(infoTable, "Tip:", formatPrescriptionType(prescriptionType));
            addInfoRow(infoTable, "Pacient:", patientName);
            addInfoRow(infoTable, "Data nașterii:", patientDob != null ? patientDob : "—");
            addInfoRow(infoTable, "MRN:", patientMrn != null ? patientMrn : "—");
            addInfoRow(infoTable, "Medic:", doctorName);
            addInfoRow(infoTable, "Valabilitate:",
                    formatDate(validFrom) + " — " + formatDate(validUntil));

            document.add(infoTable);
            document.add(Chunk.NEWLINE);

            // ── Refraction Table ────────────────────────────────────────
            document.add(new Paragraph("Refracție", FONT_SUBTITLE));
            document.add(new Paragraph(" ", FONT_SMALL));

            PdfPTable refrTable = new PdfPTable(8);
            refrTable.setWidthPercentage(100);
            refrTable.setWidths(new float[]{1.2f, 1, 1, 1, 1, 1, 1, 1});

            // Table header
            String[] headers = {"Ochi", "Sph", "Cyl", "Axis", "Add", "VA cc", "BCVA", "SEQ"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FONT_HEADER));
                cell.setBackgroundColor(TABLE_HEADER_BG);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                refrTable.addCell(cell);
            }

            // Data rows
            for (RefractionLine line : lines) {
                Color eyeColor = "OD".equals(line.eye()) ? OD_COLOR : OS_COLOR;
                String eyeLabel = "OD".equals(line.eye()) ? "OD (drept)" : "OS (stâng)";

                PdfPCell eyeCell = new PdfPCell(new Phrase(eyeLabel, new Font(Font.HELVETICA, 10, Font.BOLD, eyeColor)));
                eyeCell.setPadding(5);
                refrTable.addCell(eyeCell);

                addMonoCell(refrTable, formatDiopter(line.sph()));
                addMonoCell(refrTable, formatDiopter(line.cyl()));
                addMonoCell(refrTable, line.axis() != null ? line.axis() + "°" : "—");
                addMonoCell(refrTable, formatDiopter(line.addPower()));
                addMonoCell(refrTable, line.vaCc() != null ? line.vaCc() : "—");
                addMonoCell(refrTable, line.bcva() != null ? line.bcva() : "—");
                addMonoCell(refrTable, formatDiopter(line.seq()));
            }

            document.add(refrTable);
            document.add(Chunk.NEWLINE);

            // ── PD Values ───────────────────────────────────────────────
            StringBuilder pdText = new StringBuilder("Distanță pupilară: ");
            if (pdBinocular != null) {
                pdText.append("Binoculară ").append(pdBinocular).append(" mm");
            }
            if (pdOd != null && pdOs != null) {
                if (pdBinocular != null) pdText.append("  |  ");
                pdText.append("OD ").append(pdOd).append(" mm / OS ").append(pdOs).append(" mm");
            }
            document.add(new Paragraph(pdText.toString(), FONT_BODY));

            // ── Lens Specifications ─────────────────────────────────────
            if (lensType != null || lensMaterial != null || lensCoating != null) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Specificații lentile", FONT_SUBTITLE));
                if (lensType != null) {
                    document.add(new Paragraph("Tip: " + lensType, FONT_BODY));
                }
                if (lensMaterial != null) {
                    document.add(new Paragraph("Material: " + lensMaterial, FONT_BODY));
                }
                if (lensCoating != null) {
                    document.add(new Paragraph("Tratament: " + lensCoating, FONT_BODY));
                }
            }

            // ── Clinical Notes ──────────────────────────────────────────
            if (clinicalNotes != null && !clinicalNotes.isBlank()) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Note clinice", FONT_SUBTITLE));
                document.add(new Paragraph(clinicalNotes, FONT_BODY));
            }

            // ── Patient Instructions ────────────────────────────────────
            if (patientInstructions != null && !patientInstructions.isBlank()) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Instrucțiuni pacient", FONT_SUBTITLE));
                document.add(new Paragraph(patientInstructions, FONT_BODY));
            }

            // ── QR Code ─────────────────────────────────────────────────
            document.add(Chunk.NEWLINE);
            String qrUrl = QR_VERIFY_BASE_URL + qrCodeToken;
            byte[] qrBytes = qrCodeGenerator.generatePng(qrUrl);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.scaleToFit(120, 120);
            qrImage.setAlignment(Element.ALIGN_RIGHT);
            document.add(qrImage);
            document.add(new Paragraph("Scanați codul QR pentru verificare online", FONT_SMALL));

            // ── Footer ──────────────────────────────────────────────────
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("─".repeat(80), FONT_SMALL));
            document.add(new Paragraph(
                    "Document generat electronic de " + clinicName + " • " + formatInstant(Instant.now()),
                    FONT_SMALL));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate prescription PDF: prescription={}", prescriptionNumber, e);
            throw new RuntimeException("Prescription PDF generation failed", e);
        }
    }

    // ── Invoice PDF ─────────────────────────────────────────────────────────

    /**
     * Generates an invoice PDF document per Romanian fiscal format.
     *
     * @param clinicName     clinic name
     * @param invoiceNumber  invoice number (e.g. "FCT/2026/000001/A")
     * @param patientName    patient full name
     * @param issuedAt       invoice issue date
     * @param status         DRAFT, ISSUED, PAID, etc.
     * @param paymentMethod  payment method (if paid)
     * @param currency       currency code (e.g. "RON")
     * @param subtotal       subtotal before VAT
     * @param vatTotal       total VAT
     * @param discountTotal  total discounts
     * @param total          grand total
     * @param paidAt         payment timestamp (nullable)
     * @param lines          invoice line items
     * @return PDF bytes
     */
    public byte[] generateInvoicePdf(
            String clinicName,
            String invoiceNumber,
            String patientName,
            Instant issuedAt,
            String status,
            String paymentMethod,
            String currency,
            BigDecimal subtotal,
            BigDecimal vatTotal,
            BigDecimal discountTotal,
            BigDecimal total,
            Instant paidAt,
            List<InvoiceLine> lines) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            // ── Header ──────────────────────────────────────────────────
            addClinicHeader(document, clinicName);
            document.add(new Paragraph("FACTURĂ", FONT_TITLE));
            document.add(Chunk.NEWLINE);

            // ── Invoice info ────────────────────────────────────────────
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1, 1});

            addInfoRow(infoTable, "Nr. factură:", invoiceNumber);
            addInfoRow(infoTable, "Data emiterii:", issuedAt != null ? formatInstant(issuedAt) : "—");
            addInfoRow(infoTable, "Client:", patientName != null ? patientName : "—");
            addInfoRow(infoTable, "Status:", status);
            if (paidAt != null) {
                addInfoRow(infoTable, "Data plată:", formatInstant(paidAt));
                addInfoRow(infoTable, "Metoda plată:", paymentMethod != null ? paymentMethod : "—");
            }

            document.add(infoTable);
            document.add(Chunk.NEWLINE);

            // ── Line Items Table ────────────────────────────────────────
            document.add(new Paragraph("Detalii facturare", FONT_SUBTITLE));
            document.add(new Paragraph(" ", FONT_SMALL));

            PdfPTable lineTable = new PdfPTable(6);
            lineTable.setWidthPercentage(100);
            lineTable.setWidths(new float[]{3, 0.8f, 1.2f, 1, 1, 1.3f});

            String[] lineHeaders = {"Descriere", "Cant.", "Preț unit.", "Disc. %", "TVA %", "Total linie"};
            for (String h : lineHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FONT_HEADER));
                cell.setBackgroundColor(TABLE_HEADER_BG);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                lineTable.addCell(cell);
            }

            for (InvoiceLine line : lines) {
                addBodyCell(lineTable, line.description(), Element.ALIGN_LEFT);
                addBodyCell(lineTable, String.valueOf(line.quantity()), Element.ALIGN_CENTER);
                addBodyCell(lineTable, formatMoney(line.unitPrice()), Element.ALIGN_RIGHT);
                addBodyCell(lineTable, line.discountPercent() != null ? line.discountPercent() + "%" : "0%", Element.ALIGN_CENTER);
                addBodyCell(lineTable, line.vatRate() != null ? line.vatRate() + "%" : "19%", Element.ALIGN_CENTER);
                addBodyCell(lineTable, formatMoney(line.lineTotal()), Element.ALIGN_RIGHT);
            }

            document.add(lineTable);
            document.add(Chunk.NEWLINE);

            // ── Totals ──────────────────────────────────────────────────
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(50);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addTotalRow(totalsTable, "Subtotal:", formatMoney(subtotal) + " " + currency);
            if (discountTotal != null && discountTotal.compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "Discount:", "-" + formatMoney(discountTotal) + " " + currency);
            }
            addTotalRow(totalsTable, "TVA:", formatMoney(vatTotal) + " " + currency);

            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL:", new Font(Font.HELVETICA, 12, Font.BOLD)));
            totalLabel.setBorderWidth(0);
            totalLabel.setPadding(5);
            totalLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
            totalsTable.addCell(totalLabel);

            PdfPCell totalValue = new PdfPCell(new Phrase(
                    formatMoney(total) + " " + currency,
                    new Font(Font.HELVETICA, 12, Font.BOLD)));
            totalValue.setBorderWidth(0);
            totalValue.setPadding(5);
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.addCell(totalValue);

            document.add(totalsTable);

            // ── Footer ──────────────────────────────────────────────────
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("─".repeat(80), FONT_SMALL));
            document.add(new Paragraph(
                    "Document generat electronic de " + clinicName + " • " + formatInstant(Instant.now()),
                    FONT_SMALL));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate invoice PDF: invoice={}", invoiceNumber, e);
            throw new RuntimeException("Invoice PDF generation failed", e);
        }
    }

    // ── Helper Methods ──────────────────────────────────────────────────────

    private void addClinicHeader(Document document, String clinicName) throws DocumentException {
        Paragraph header = new Paragraph(clinicName != null ? clinicName : "OphthaCloud Clinic", FONT_TITLE);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);
        document.add(new Paragraph("─".repeat(80), FONT_SMALL));
        document.add(Chunk.NEWLINE);
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_HEADER));
        labelCell.setBorderWidth(0);
        labelCell.setPadding(3);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "—", FONT_BODY));
        valueCell.setBorderWidth(0);
        valueCell.setPadding(3);
        table.addCell(valueCell);
    }

    private void addMonoCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FONT_MONO));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String value, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "—", FONT_BODY));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_BODY));
        labelCell.setBorderWidth(0);
        labelCell.setPadding(3);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_BODY));
        valueCell.setBorderWidth(0);
        valueCell.setPadding(3);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private String formatDiopter(BigDecimal value) {
        if (value == null) return "—";
        return (value.signum() >= 0 ? "+" : "") + value.setScale(2).toPlainString();
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2).toPlainString();
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "—";
        return date.format(DATE_FMT);
    }

    private String formatInstant(Instant instant) {
        if (instant == null) return "—";
        return instant.atZone(BUCHAREST).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    private String formatPrescriptionType(String type) {
        if (type == null) return "—";
        return switch (type) {
            case "DISTANCE" -> "Distanță";
            case "NEAR" -> "Aproape";
            case "PROGRESSIVE" -> "Progresiv";
            case "BIFOCAL" -> "Bifocal";
            case "CONTACT_LENS" -> "Lentile contact";
            default -> type;
        };
    }

    // ── Data Records ────────────────────────────────────────────────────────

    /**
     * Refraction line data for prescription PDF generation.
     */
    public record RefractionLine(
            String eye,
            BigDecimal sph,
            BigDecimal cyl,
            Short axis,
            BigDecimal addPower,
            String vaCc,
            String bcva,
            BigDecimal seq
    ) {}

    /**
     * Invoice line data for invoice PDF generation.
     */
    public record InvoiceLine(
            String description,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal discountPercent,
            BigDecimal vatRate,
            BigDecimal lineTotal
    ) {}
}
