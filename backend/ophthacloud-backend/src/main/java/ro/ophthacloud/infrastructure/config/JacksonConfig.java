package ro.ophthacloud.infrastructure.config;

import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return tools.jackson.databind.json.JsonMapper.builder().build();
    }
}
