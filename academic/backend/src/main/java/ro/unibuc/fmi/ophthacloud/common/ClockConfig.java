package ro.unibuc.fmi.ophthacloud.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    public Clock clinicClock(@Value("${app.clinic-zone:Europe/Bucharest}") String clinicZone) {
        return Clock.system(ZoneId.of(clinicZone));
    }
}
