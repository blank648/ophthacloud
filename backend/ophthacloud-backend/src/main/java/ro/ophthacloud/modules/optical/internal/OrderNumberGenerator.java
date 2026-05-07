package ro.ophthacloud.modules.optical.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private final OpticalOrderRepository orderRepository;

    /**
     * Generates an order number formatted as CMD-{YYYY}-{000001}.
     * The sequence resets annually per tenant.
     */
    public String generate(UUID tenantId, Instant createdAt) {
        int year = createdAt.atZone(ZoneId.of("Europe/Bucharest")).getYear();
        long nextSequence = orderRepository.countByTenantIdAndYear(tenantId, year) + 1;
        return String.format("CMD-%d-%06d", year, nextSequence);
    }
}
