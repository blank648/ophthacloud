package ro.ophthacloud;

import org.springframework.boot.SpringApplication;

public class TestOphthacloudBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(OphthacloudBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
