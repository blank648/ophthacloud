package ro.ophthacloud.modules.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ro.ophthacloud.modules.admin.internal.KeycloakAdminService;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Admin module controllers.
 * <p>
 * Keycloak is mocked via {@link MockitoBean} so no real Keycloak server is
 * needed.
 * Covers: staff CRUD with mocked Keycloak, permissions GET/PUT,
 * clinic settings GET/PUT (valid + validation failure), audit log GET.
 */
@DisplayName("Admin module integration tests")
class AdminControllerIntegrationTest extends BaseIntegrationTest {

        @MockitoBean
        private KeycloakAdminService keycloakAdminService;

        private static final UUID TENANT_A = UUID.randomUUID();

        @BeforeEach
        void setUpAdminData() {
                ensureTenantExists(TENANT_A);
                cleanAdminTables();
                seedClinicSettings(TENANT_A);
                stubKeycloak();
        }

        // ── Test 1: POST /api/v1/admin/staff → 201 (Keycloak mocked) ────────────

        @Test
        @DisplayName("POST /api/v1/admin/staff: should return 201 with created staff member")
        void createStaff_shouldReturn201_withMockedKeycloak() {
                Map<String, Object> body = Map.of(
                                "firstName", "Ioana",
                                "lastName", "Popa",
                                "email", "dr.popa@clinic.ro",
                                "phone", "0733456789",
                                "role", "DOCTOR",
                                "specialization", "Oftalmologie",
                                "licenseNumber", "CMR-12345",
                                "sendInviteEmail", false);

                ResponseEntity<Map<String, Object>> response = client.post()
                                .uri("/api/v1/admin/staff")
                                .headers(h -> h.addAll(headersForRole("CLINIC_ADMIN", TENANT_A)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(body)
                                .retrieve()
                                .toEntity(new ParameterizedTypeReference<>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
                assertThat(data).isNotNull();
                assertThat(data.get("email")).isEqualTo("dr.popa@clinic.ro");
                assertThat(data.get("role")).isEqualTo("DOCTOR");
                assertThat(data.get("isActive")).isEqualTo(true);
        }

        // ── Test 2: POST staff with missing required field → 400 ─────────────────

        @Test
        @DisplayName("POST /api/v1/admin/staff: should return 400 when email is missing")
        void createStaff_shouldReturn400_whenEmailMissing() {
                Map<String, Object> body = Map.of(
                                "firstName", "Ioana",
                                "lastName", "Popa",
                                "role", "DOCTOR",
                                "sendInviteEmail", false
                // email missing — @NotBlank violation
                );

                ResponseEntity<Map<String, Object>> response = client.post()
                                .uri("/api/v1/admin/staff")
                                .headers(h -> h.addAll(headersForRole("CLINIC_ADMIN", TENANT_A)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(body)
                                .retrieve()
                                .toEntity(new ParameterizedTypeReference<>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                Map<?, ?> error = (Map<?, ?>) response.getBody().get("error");
                assertThat(error).isNotNull();
                assertThat(error.get("code")).isEqualTo("VALIDATION_ERROR");
        }

        // ── Test 3: GET /api/v1/admin/permissions?role=DOCTOR ────────────────────

        @Test
        @DisplayName("GET /api/v1/admin/permissions: should return empty list for role with no permissions")
        void getPermissions_shouldReturn200_withEmptyListForUnseededRole() {
                ResponseEntity<Map<String, Object>> response = client.get()
                                .uri("/api/v1/admin/permissions?role=DOCTOR")
                                .headers(h -> h.addAll(headersForRole("CLINIC_ADMIN", TENANT_A)))
                                .retrieve()
                                .toEntity(new ParameterizedTypeReference<>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                List<?> data = (List<?>) response.getBody().get("data");
                assertThat(data).isNotNull().isEmpty();
        }

        // ── Test 4: PUT /api/v1/admin/permissions → 200 + DB updated ─────────────

        @Test
        @DisplayName("PUT /api/v1/admin/permissions: should return 200 with updated permissions")
        void updatePermissions_shouldReturn200_andPersistPermissions() throws Exception {
                Map<String, Object> body = Map.of(
                                "role", "RECEPTIONIST",
                                "permissions", List.of(
                                                Map.of("moduleCode", "patients", "canView", true, "canCreate", false,
                                                                "canEdit", false, "canDelete", false, "canSign", false,
                                                                "canExport", false),
                                                Map.of("moduleCode", "appointments", "canView", true, "canCreate", true,
                                                                "canEdit", true, "canDelete", false, "canSign", false,
                                                                "canExport", false)));

                // First PUT to set permissions
                ResponseEntity<Map<String, Object>> putResponse = client.put()
                                .uri("/api/v1/admin/permissions")
                                .headers(h -> h.addAll(headersForRole("CLINIC_ADMIN", TENANT_A)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(body)
                                .retrieve()
                                .toEntity(new ParameterizedTypeReference<>() {
                                });

                assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                List<?> data = (List<?>) putResponse.getBody().get("data");
                assertThat(data).isNotNull().hasSize(2);

                // Verify DB updated — subsequent GET returns the same data
                ResponseEntity<Map<String, Object>> getResponse = client.get()
                                .uri("/api/v1/admin/permissions?role=RECEPTIONIST")
                                .headers(h -> h.addAll(headersForRole("CLINIC_ADMIN", TENANT_A)))
                                .retrieve()
                                .toEntity(new ParameterizedTypeReference<>() {
                                });

                assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                List<?> getdata = (List<?>) getResponse.getBody().get("data");
                assertThat(getdata).hasSize(2);
        }

        // ── Test 5: GET /api/v1/admin/settings → 200 ────────────────────────────

        @Test
        @DisplayName("GET /api/v1/admin/settings: should return 200 with default settings")
        void getSettings_shouldReturn200_withDefaultValues() {
                ResponseEntity<Map<String, Object>> response = client.get()
                                .uri("/api/v1/admin/settings")
                                .headers(h -> h.addAll(headersForRole("CLINIC_ADMIN", TENANT_A)))
                                .retrieve()
                                .toEntity(new ParameterizedTypeReference<>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
                assertThat(data).isNotNull();
                assertThat(data.get("bookingSlotMinutes")).isEqualTo(15);
                assertThat(data.get("currency")).isEqualTo("RON");
        }

        // ── Test 6: PUT /api/v1/admin/settings → 200 (valid data) ───────────────

        @Test
        @DisplayName("PUT /api/v1/admin/settings: should return 200 with updated settings")
        void updateSettings_shouldReturn200_withValidData() {
                Map<String, Object> body = Map.of(
                                "bookingSlotMinutes", 30,
                                "bookingAdvanceDays", 60,
                                "vatRateDefault", 19);

                ResponseEntity<Map<String, Object>> response = client.put()
                                .uri("/api/v1/admin/settings")
                                .headers(h -> h.addAll(headersForRole("CLINIC_ADMIN", TENANT_A)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(body)
                                .retrieve()
                                .toEntity(new ParameterizedTypeReference<>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
                assertThat(data.get("bookingSlotMinutes")).isEqualTo(30);
        }

        // ── Test 7: PUT /api/v1/admin/settings → 400 (invalid bookingSlotMinutes=13) ─

        @Test
        @DisplayName("PUT /api/v1/admin/settings: should return 400 for bookingSlotMinutes=13 (not in allowed set)")
        void updateSettings_shouldReturn400_forInvalidSlotMinutes() {
                Map<String, Object> body = Map.of(
                                "bookingSlotMinutes", 13 // must be one of {10,15,20,30,45,60}
                );

                ResponseEntity<Map<String, Object>> response = client.put()
                                .uri("/api/v1/admin/settings")
                                .headers(h -> h.addAll(headersForRole("CLINIC_ADMIN", TENANT_A)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(body)
                                .retrieve()
                                .toEntity(new ParameterizedTypeReference<>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                Map<?, ?> error = (Map<?, ?>) response.getBody().get("error");
                assertThat(error).isNotNull();
                // IllegalArgumentException from ClinicSettingsService → "BAD_REQUEST" per
                // GlobalExceptionHandler
                assertThat(error.get("code")).isEqualTo("BAD_REQUEST");
        }

        // ── Test 8: GET /api/v1/audit/log → 200 paginated ───────────────────────

        @Test
        @DisplayName("GET /api/v1/audit/log: should return 200 with paginated audit entries")
        void getAuditLog_shouldReturn200_withPaginatedResults() {
                ResponseEntity<Map<String, Object>> response = client.get()
                                .uri("/api/v1/audit/log?page=0&size=10")
                                .headers(h -> h.addAll(headersForRole("CLINIC_ADMIN", TENANT_A)))
                                .retrieve()
                                .toEntity(new ParameterizedTypeReference<>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                List<?> data = (List<?>) response.getBody().get("data");
                assertThat(data).isNotNull();
                Map<?, ?> pagination = (Map<?, ?>) response.getBody().get("pagination");
                assertThat(pagination).isNotNull();
                assertThat(pagination.get("page")).isEqualTo(0);
        }

        // ── Test 9: GET /api/v1/audit/log?entityType=StaffMember ────────────────

        @Test
        @DisplayName("GET /api/v1/audit/log: should return 200 filtered by entityType")
        void getAuditLog_shouldReturn200_filteredByEntityType() {
                ResponseEntity<Map<String, Object>> response = client.get()
                                .uri("/api/v1/audit/log?entityType=StaffMember")
                                .headers(h -> h.addAll(headersForRole("CLINIC_ADMIN", TENANT_A)))
                                .retrieve()
                                .toEntity(new ParameterizedTypeReference<>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                List<?> data = (List<?>) response.getBody().get("data");
                assertThat(data).isNotNull().isEmpty(); // no staff audit entries seeded
        }

        // ── Test 10: RECEPTIONIST cannot write to admin endpoints ────────────────

        @Test
        @DisplayName("RBAC: POST /api/v1/admin/staff with RECEPTIONIST should return 403")
        void createStaff_shouldReturn403_forReceptionist() {
                Map<String, Object> body = Map.of(
                                "firstName", "Test", "lastName", "User",
                                "email", "test@clinic.ro", "role", "DOCTOR",
                                "sendInviteEmail", false);

                ResponseEntity<Map<String, Object>> response = client.post()
                                .uri("/api/v1/admin/staff")
                                .headers(h -> h.addAll(headersForRole("RECEPTIONIST", TENANT_A)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(body)
                                .retrieve()
                                .toEntity(new ParameterizedTypeReference<>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        // ── Helpers ───────────────────────────────────────────────────────────────

        private void stubKeycloak() {
                String mockedKeycloakId = UUID.randomUUID().toString();
                when(keycloakAdminService.createUser(anyString(), anyString(), anyString()))
                                .thenReturn(mockedKeycloakId);
                doNothing().when(keycloakAdminService).assignTenantMemberRole(anyString());
                doNothing().when(keycloakAdminService).setAttributes(anyString(), any(), any(), any(), any());
                doNothing().when(keycloakAdminService).sendVerifyEmail(anyString());
                doNothing().when(keycloakAdminService).disableUser(anyString());
                doNothing().when(keycloakAdminService).invalidateSessions(anyString());
        }

        /**
         * Seeds a default clinic_settings row for the given tenant,
         * required by GET /api/v1/admin/settings.
         */
        private void seedClinicSettings(UUID tenantId) {
                jdbcTemplate.update("""
                                INSERT INTO clinic_settings (tenant_id, working_hours)
                                VALUES (?, '{}')
                                ON CONFLICT (tenant_id) DO NOTHING
                                """, tenantId);
        }

        private void cleanAdminTables() {
                for (String table : new String[] {
                                "tenant_role_module_permissions",
                                "staff_members",
                                "clinic_settings"
                }) {
                        try {
                                jdbcTemplate.execute("TRUNCATE TABLE " + table + " CASCADE");
                        } catch (Exception ignored) {
                        }
                }
        }
}
