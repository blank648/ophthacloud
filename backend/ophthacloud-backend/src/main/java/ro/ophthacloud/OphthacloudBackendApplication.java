package ro.ophthacloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = { DataRedisRepositoriesAutoConfiguration.class })
@EnableJpaRepositories(basePackages = "ro.ophthacloud")
public class OphthacloudBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OphthacloudBackendApplication.class, args);
    }

}
