package ro.unibuc.fmi.ophthacloud.common;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ClockConfigTest {

    @Test
    void clinicClockUsesEuropeBucharestZone() {
        ClockConfig config = new ClockConfig();
        Clock clock = config.clinicClock("Europe/Bucharest");

        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Europe/Bucharest"));

        Instant winterInstant = Instant.parse("2026-12-16T08:00:00Z");
        LocalDateTime clinicTime = LocalDateTime.ofInstant(winterInstant, clock.getZone());
        assertThat(clinicTime).isEqualTo(LocalDateTime.of(2026, 12, 16, 10, 0));
    }
}
