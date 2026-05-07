package ro.ophthacloud.modules.optical;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import ro.ophthacloud.OphthacloudBackendApplication;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Spring Modulith boundary and integration tests for the Optical module.
 */
@ApplicationModuleTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@ActiveProfiles("test")
@DisplayName("OpticalModule (Spring Modulith)")
class OpticalModuleTest {

    @Test
    @DisplayName("module_isValid: ApplicationModules.verify() should pass")
    void verifyModule() {
        assertThatCode(() ->
                ApplicationModules.of(OphthacloudBackendApplication.class).verify()
        ).doesNotThrowAnyException();
    }
}
