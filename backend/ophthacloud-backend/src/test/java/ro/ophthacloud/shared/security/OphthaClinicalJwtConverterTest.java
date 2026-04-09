package ro.ophthacloud.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OphthaClinicalJwtConverter")
class OphthaClinicalJwtConverterTest {

    private OphthaClinicalJwtConverter converter;

    private static final String TENANT_ID = "11111111-0000-0000-0000-000000000001";
    private static final String STAFF_ID  = "22222222-0000-0000-0000-000000000001";
    private static final String SUBJECT   = "kc-user-uuid-abc";

    @BeforeEach
    void setUp() {
        converter = new OphthaClinicalJwtConverter();
    }

    // ── Happy path ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("convert: should return OphthaClinicalAuthenticationToken with populated principal")
    void convert_shouldReturnToken_withPopulatedPrincipal() {
        Jwt jwt = buildJwt(Map.of(
                "tenant_id", TENANT_ID,
                "staff_id",  STAFF_ID,
                "staff_role", "DOCTOR",
                "permissions", Map.of(
                        "patients", Map.of("view", true, "create", true,
                                "edit", true, "delete", false, "sign", false, "export", true)
                )
        ));

        var token = converter.convert(jwt);

        assertThat(token).isInstanceOf(OphthaClinicalAuthenticationToken.class);
        assertThat(token.isAuthenticated()).isTrue();

        OphthaPrincipal principal = (OphthaPrincipal) token.getPrincipal();
        assertThat(principal.tenantId()).isEqualTo(TENANT_ID);
        assertThat(principal.staffId()).isEqualTo(STAFF_ID);
        assertThat(principal.staffRole()).isEqualTo("DOCTOR");
        assertThat(principal.keycloakUserId()).isEqualTo(SUBJECT);
    }

    @Test
    @DisplayName("convert: should parse permissions map correctly")
    void convert_shouldParsePermissions_correctly() {
        Jwt jwt = buildJwt(Map.of(
                "tenant_id",  TENANT_ID,
                "staff_role", "DOCTOR",
                "permissions", Map.of(
                        "emr",    Map.of("view", true, "create", true, "edit", true,
                                         "delete", false, "sign", true, "export", true),
                        "admin",  Map.of("view", false, "create", false, "edit", false,
                                         "delete", false, "sign", false, "export", false)
                )
        ));

        var token = (OphthaClinicalAuthenticationToken) converter.convert(jwt);
        OphthaPrincipal principal = token.getPrincipal();

        ModulePermissions emr = principal.permissionsFor("emr");
        assertThat(emr.view()).isTrue();
        assertThat(emr.sign()).isTrue();
        assertThat(emr.delete()).isFalse();

        ModulePermissions admin = principal.permissionsFor("admin");
        assertThat(admin.view()).isFalse();
        assertThat(admin.create()).isFalse();
    }

    @Test
    @DisplayName("convert: should assign ROLE_DOCTOR authority when staff_role is DOCTOR")
    void convert_shouldAssignRoleAuthority_fromStaffRole() {
        Jwt jwt = buildJwt(Map.of(
                "tenant_id",  TENANT_ID,
                "staff_role", "DOCTOR"
        ));

        var token = converter.convert(jwt);

        assertThat(token.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_DOCTOR");
    }

    @Test
    @DisplayName("convert: should return empty permissions when claim is absent")
    void convert_shouldReturnEmptyPermissions_whenPermissionsClaimAbsent() {
        Jwt jwt = buildJwt(Map.of(
                "tenant_id",  TENANT_ID,
                "staff_role", "NURSE"
        ));

        var token = (OphthaClinicalAuthenticationToken) converter.convert(jwt);
        OphthaPrincipal principal = token.getPrincipal();

        ModulePermissions patients = principal.permissionsFor("patients");
        assertThat(patients).isEqualTo(ModulePermissions.NONE);
    }

    // ── Rejection cases ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("convert: should throw InvalidBearerTokenException when tenant_id is missing")
    void convert_shouldThrow_whenTenantIdMissing() {
        Jwt jwt = buildJwt(Map.of("staff_role", "DOCTOR"));

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(InvalidBearerTokenException.class)
                .hasMessageContaining("tenant_id");
    }

    @Test
    @DisplayName("convert: should throw InvalidBearerTokenException when tenant_id is blank")
    void convert_shouldThrow_whenTenantIdBlank() {
        Jwt jwt = buildJwt(Map.of("tenant_id", "  ", "staff_role", "DOCTOR"));

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private static Jwt buildJwt(Map<String, Object> extraClaims) {
        Map<String, Object> claims = new java.util.HashMap<>(extraClaims);
        claims.putIfAbsent("sub", SUBJECT);
        claims.putIfAbsent("iss", "http://localhost:8180/realms/ophthacloud");

        return Jwt.withTokenValue("dummy-token-value")
                .headers(h -> h.put("alg", "RS256"))
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
    }
}
