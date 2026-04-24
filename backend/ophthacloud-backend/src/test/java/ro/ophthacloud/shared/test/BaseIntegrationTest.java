package ro.ophthacloud.shared.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

/**
 * Abstract base for all OphthaCloud integration tests.
 *
 * <p>Spins up:
 * <ul>
 *   <li>PostgreSQL 16 (alpine) via Testcontainers</li>
 *   <li>Redis 7 (alpine) via Testcontainers</li>
 * </ul>
 *
 * <p>Provides:
 * <ul>
 *   <li>{@link #baseUrl()} — the random-port base URL for constructing requests</li>
 *   <li>{@link #client} — a Spring {@link RestClient} pre-configured with the server base URL</li>
 *   <li>{@link #headersForRole(String, UUID)} — signed Bearer JWT headers</li>
 *   <li>{@link #dbCleanup()} — table truncation before each test</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    // ── Testcontainers ────────────────────────────────────────────────────────

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withReuse(true);

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379)
                    .withReuse(true);

    // ── Dynamic properties ────────────────────────────────────────────────────

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user",     POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());
    }

    // ── Injected beans ────────────────────────────────────────────────────────

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /** Spring RestClient — fault-tolerant (does not throw on 4xx/5xx). */
    protected RestClient client;

    /**
     * Re-creates the {@link RestClient} after Spring injects {@link #port}.
     * Also truncates tables for a clean DB state before each test.
     */
    @BeforeEach
    void setUp() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, resp) -> { /* never throw */ })
                .build();
        dbCleanup();
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    /**
     * Truncates test-relevant tables in FK-safe order.
     * Wrapped in try/catch so tables absent in early sprints don't fail the suite.
     */
    void dbCleanup() {
        for (String table : new String[]{
                "appointments", "blocked_slots", "appointment_types", "patients", "audit_log"}) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE " + table + " CASCADE");
            } catch (Exception ignored) { /* table may not exist yet */ }
        }
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    /**
     * Returns the base URL for this test's server instance (random port).
     */
    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Builds {@link HttpHeaders} with a signed HS256 Bearer JWT for the given role + tenant.
     *
     * @param role     e.g. {@code "DOCTOR"}, {@code "ADMIN"}, {@code "RECEPTIONIST"}
     * @param tenantId tenant UUID embedded in the JWT {@code tenant_id} claim
     */
    protected HttpHeaders headersForRole(String role, UUID tenantId) {
        String staffId = UUID.randomUUID().toString();
        String token   = TestJwtFactory.createToken(role, tenantId, staffId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    /**
     * Inserts a minimal tenant row — idempotent (ON CONFLICT DO NOTHING).
     */
    protected void ensureTenantExists(UUID tenantId) {
        jdbcTemplate.update("""
                INSERT INTO tenants (id, slug, name, keycloak_realm)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                tenantId,
                "test-" + tenantId.toString().substring(0, 8),
                "Test Clinic",
                "ophthacloud-test"
        );
    }
}
