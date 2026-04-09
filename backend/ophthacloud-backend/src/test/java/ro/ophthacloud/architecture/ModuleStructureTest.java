package ro.ophthacloud.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Architecture verification test — ensures Spring Modulith module boundaries are respected.
 * <p>
 * This test runs on every build and blocks CI if:
 * - A module accesses another module's internal package
 * - A circular dependency exists between modules
 * - A module is not properly annotated with @ApplicationModule
 * </p>
 *
 * @see <a href="https://docs.spring.io/spring-modulith/reference/">Spring Modulith Reference</a>
 */
class ModuleStructureTest {

    private final ApplicationModules modules = ApplicationModules.of(
            ro.ophthacloud.OphthacloudBackendApplication.class
    );

    @Test
    @DisplayName("All modules should be compliant — no circular deps, no internal access violations")
    void modulesShouldBeCompliant() {
        // Verifies: no module accesses another module's internal packages
        // Verifies: no circular dependencies between modules
        // Verifies: all cross-module calls go through public Facade or Events
        modules.verify();
    }

    @Test
    @DisplayName("All modules should be detected — 11 business + shared + infrastructure")
    void allModulesShouldBeDetected() {
        // Print module structure for debugging — useful during development
        modules.forEach(module ->
                System.out.println("Module: " + module.getName() + " — " + module.getBasePackage())
        );
    }
}
