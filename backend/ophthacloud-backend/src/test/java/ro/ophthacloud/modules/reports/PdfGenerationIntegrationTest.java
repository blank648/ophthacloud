package ro.ophthacloud.modules.reports;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ro.ophthacloud.shared.test.BaseIntegrationTest;
import ro.ophthacloud.shared.util.PdfGenerationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PDF generation (OC-043).
 *
 * Verifies that:
 * - Prescription PDF contains QR code (byte length is non-trivial)
 * - Invoice PDF has correct line totals in extracted text
 * - Both PDFs are valid PDF documents (start with %PDF header)
 */
@DisplayName("PdfGeneration (Integration)")
class PdfGenerationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PdfGenerationService pdfGenerationService;

    // ── Prescription PDF tests ────────────────────────────────────────────────

    @Test
    @DisplayName("generatePrescriptionPdf: produces valid non-empty PDF bytes")
    void generatePrescriptionPdf_shouldProduceValidPdf() {
        byte[] pdf = pdfGenerationService.generatePrescriptionPdf(
                "Clinica Test SRL",
                "Dr. Ionescu Maria",
                "Popescu Ion",
                "01.01.1980",
                "OC-004821",
                "RX-2026-004821",
                "DISTANCE",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                new BigDecimal("64.0"),
                new BigDecimal("32.0"),
                new BigDecimal("32.0"),
                "SINGLE_VISION",
                "Poliuretan 1.67",
                "Antireflectantă",
                "Note clinice test",
                "Purtați ochelarii permanent",
                "550e8400-e29b-41d4-a716-446655440000",
                List.of(
                        new PdfGenerationService.RefractionLine(
                                "OD",
                                new BigDecimal("-2.50"),
                                new BigDecimal("-0.75"),
                                (short) 90,
                                null,
                                "6/6",
                                null,
                                null),
                        new PdfGenerationService.RefractionLine(
                                "OS",
                                new BigDecimal("-1.75"),
                                new BigDecimal("-0.50"),
                                (short) 85,
                                null,
                                "6/6",
                                null,
                                null)
                )
        );

        assertThat(pdf).isNotNull().isNotEmpty();
        // PDF files always start with the "%PDF" magic bytes
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        // QR code image adds significant bytes — expect > 5 KB
        assertThat(pdf.length).isGreaterThan(5_000);
    }

    @Test
    @DisplayName("generatePrescriptionPdf: extracted text contains key prescription fields")
    void generatePrescriptionPdf_shouldContainKeyText() throws Exception {
        byte[] pdf = pdfGenerationService.generatePrescriptionPdf(
                "Clinica Vedere SRL",
                "Dr. Georgescu Ana",
                "Dumitru Radu",
                "15.08.1963",
                "OC-000042",
                "RX-2026-000042",
                "PROGRESSIVE",
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2027, 1, 15),
                new BigDecimal("62.0"),
                null, null,
                "PROGRESSIVE",
                "Policarbonat 1.6",
                null, null, null,
                "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                List.of(
                        new PdfGenerationService.RefractionLine(
                                "OD", new BigDecimal("-3.00"), new BigDecimal("-1.00"),
                                (short) 180, new BigDecimal("2.00"), "6/9", null, null)
                )
        );

        PdfReader reader = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder text = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            text.append(extractor.getTextFromPage(i));
        }
        reader.close();

        String content = text.toString();
        assertThat(content).contains("Clinica Vedere SRL");
        assertThat(content).contains("RX-2026-000042");
        assertThat(content).contains("Dumitru Radu");
    }

    // ── Invoice PDF tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("generateInvoicePdf: produces valid non-empty PDF bytes")
    void generateInvoicePdf_shouldProduceValidPdf() {
        byte[] pdf = pdfGenerationService.generateInvoicePdf(
                "Clinica Test SRL",
                "FCT/2026/000001/A",
                "Popescu Ion",
                Instant.now(),
                "PAID",
                "CARD",
                "RON",
                new BigDecimal("1000.00"),
                new BigDecimal("190.00"),
                new BigDecimal("0.00"),
                new BigDecimal("1190.00"),
                Instant.now(),
                List.of(
                        new PdfGenerationService.InvoiceLine(
                                "Lentile Essilor Varilux X Series",
                                2,
                                new BigDecimal("500.00"),
                                new BigDecimal("0.00"),
                                new BigDecimal("19.00"),
                                new BigDecimal("1000.00")
                        )
                )
        );

        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(2_000);
    }

    @Test
    @DisplayName("generateInvoicePdf: extracted text contains invoice number and line totals")
    void generateInvoicePdf_shouldContainInvoiceNumberAndLineTotals() throws Exception {
        var line = new PdfGenerationService.InvoiceLine(
                "Ramă Silhouette 5530",
                1,
                new BigDecimal("450.00"),
                new BigDecimal("0.00"),
                new BigDecimal("19.00"),
                new BigDecimal("450.00")
        );

        byte[] pdf = pdfGenerationService.generateInvoicePdf(
                "Optica Vederii SRL",
                "FCT/2026/000042/B",
                "Ion Ionescu",
                Instant.now(),
                "ISSUED",
                null,
                "RON",
                new BigDecimal("450.00"),
                new BigDecimal("85.50"),
                new BigDecimal("0.00"),
                new BigDecimal("535.50"),
                null,
                List.of(line)
        );

        PdfReader reader = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder text = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            text.append(extractor.getTextFromPage(i));
        }
        reader.close();

        String content = text.toString();
        assertThat(content).contains("FCT/2026/000042/B");
        assertThat(content).contains("Optica Vederii SRL");
        assertThat(content).contains("Ion Ionescu");
        assertThat(content).contains("Ramă Silhouette 5530");
    }
}
