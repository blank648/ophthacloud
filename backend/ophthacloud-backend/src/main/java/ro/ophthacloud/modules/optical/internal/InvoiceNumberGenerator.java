package ro.ophthacloud.modules.optical.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepository;

    /**
     * Generates an invoice number formatted as {PREFIX}/{YYYY}/{000001}/{PREFIX}
     * Assuming standard default prefix "INV".
     */
    public String generate(UUID tenantId, Instant createdAt) {
        int year = createdAt.atZone(ZoneId.of("Europe/Bucharest")).getYear();
        String prefix = String.format("INV/%d/", year);
        String maxInvoice = invoiceRepository.findMaxInvoiceNumberByTenantIdAndPrefix(tenantId, prefix + "%");
        long nextSequence = 1;
        if (maxInvoice != null && maxInvoice.startsWith(prefix)) {
            try {
                String suffix = maxInvoice.substring(prefix.length(), maxInvoice.length() - 4);
                nextSequence = Long.parseLong(suffix) + 1;
            } catch (Exception e) {
                nextSequence = invoiceRepository.countByTenantIdAndYear(tenantId, year) + 1;
            }
        } else {
            nextSequence = invoiceRepository.countByTenantIdAndYear(tenantId, year) + 1;
        }
        return String.format("INV/%d/%06d/INV", year, nextSequence);
    }
}
