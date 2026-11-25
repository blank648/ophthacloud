package ro.unibuc.fmi.ophthacloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class OphthacloudAcademicApplication {

    public static void main(String[] args) {
        SpringApplication.run(OphthacloudAcademicApplication.class, args);
    }
}
