package ro.ophthacloud.modules.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.modulith.core.ApplicationModules;
import ro.ophthacloud.OphthacloudBackendApplication;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Spring Modulith structural test for the {@code admin} module.
 * <p>
 * Verifies:
 * <ul>
 *   <li>No illegal cross-module dependencies into internal packages</li>
 *   <li>No cycles between modules</li>
 *   <li>Module boundary declared in package-info.java is respected</li>
 * </ul>
 */
@DisplayName("AdminModule (Spring Modulith)")
class AdminModuleTest {

    @Test
    @DisplayName("module_isValid: ApplicationModules.verify() should pass without exception")
    void module_isValid() {
        assertThatCode(() ->
                ApplicationModules.of(OphthacloudBackendApplication.class).verify()
        ).doesNotThrowAnyException();
    }
}
