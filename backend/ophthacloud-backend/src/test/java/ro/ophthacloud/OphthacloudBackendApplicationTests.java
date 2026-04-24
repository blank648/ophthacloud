package ro.ophthacloud;

import org.junit.jupiter.api.Test;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

/**
 * Smoke test — verifies the full Spring application context loads cleanly.
 * <p>
 * Inherits the singleton Testcontainers PostgreSQL + Redis instance from
 * {@link BaseIntegrationTest} so no separate context or container is created.
 */
class OphthacloudBackendApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
    }

}
