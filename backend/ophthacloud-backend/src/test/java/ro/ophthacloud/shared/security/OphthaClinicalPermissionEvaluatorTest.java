package ro.ophthacloud.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OphthaClinicalPermissionEvaluator")
class OphthaClinicalPermissionEvaluatorTest {

    private OphthaClinicalPermissionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new OphthaClinicalPermissionEvaluator();
    }

    // ── hasPermission(auth, targetDomainObject, permission) ──────────────────────

    @Test
    @DisplayName("hasPermission(domain): should always return false — unused form")
    void hasPermission_domainForm_shouldAlwaysReturnFalse() {
        var auth = buildAuth("DOCTOR", Map.of("patients",
                new ModulePermissions(true, true, true, true, true, true)));
        assertThat(evaluator.hasPermission(auth, new Object(), "VIEW")).isFalse();
    }

    // ── hasPermission(auth, moduleCode, targetType, permission) ──────────────────

    @Test
    @DisplayName("hasPermission: should return true when permission is granted")
    void hasPermission_shouldReturnTrue_whenGranted() {
        var auth = buildAuth("DOCTOR", Map.of("patients",
                new ModulePermissions(true, true, false, false, false, true)));
        assertThat(evaluator.hasPermission(auth, "patients", "MODULE", "VIEW")).isTrue();
        assertThat(evaluator.hasPermission(auth, "patients", "MODULE", "CREATE")).isTrue();
        assertThat(evaluator.hasPermission(auth, "patients", "MODULE", "EXPORT")).isTrue();
    }

    @Test
    @DisplayName("hasPermission: should return false when permission is denied")
    void hasPermission_shouldReturnFalse_whenDenied() {
        var auth = buildAuth("DOCTOR", Map.of("patients",
                new ModulePermissions(true, true, false, false, false, false)));
        assertThat(evaluator.hasPermission(auth, "patients", "MODULE", "EDIT")).isFalse();
        assertThat(evaluator.hasPermission(auth, "patients", "MODULE", "DELETE")).isFalse();
        assertThat(evaluator.hasPermission(auth, "patients", "MODULE", "SIGN")).isFalse();
    }

    @ParameterizedTest(name = "action={0}")
    @ValueSource(strings = {"VIEW", "CREATE", "EDIT", "DELETE", "SIGN", "EXPORT"})
    @DisplayName("hasPermission: should evaluate all 6 actions correctly when all granted")
    void hasPermission_shouldEvaluateAllSixActions(String action) {
        var auth = buildAuth("CLINIC_ADMIN", Map.of("emr",
                new ModulePermissions(true, true, true, true, true, true)));
        assertThat(evaluator.hasPermission(auth, "emr", "MODULE", action)).isTrue();
    }

    @Test
    @DisplayName("hasPermission: should return false when module is not in permissions map")
    void hasPermission_shouldReturnFalse_whenModuleAbsent() {
        var auth = buildAuth("DOCTOR", Map.of("patients",
                new ModulePermissions(true, true, true, false, false, false)));
        assertThat(evaluator.hasPermission(auth, "admin", "MODULE", "VIEW")).isFalse();
    }

    @Test
    @DisplayName("hasPermission: should return false when principal is not OphthaPrincipal")
    void hasPermission_shouldReturnFalse_whenPrincipalIsWrongType() {
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "plainUser", "pass", List.of());
        assertThat(evaluator.hasPermission(auth, "patients", "MODULE", "VIEW")).isFalse();
    }

    @Test
    @DisplayName("hasPermission: should return false when auth is null")
    void hasPermission_shouldReturnFalse_whenAuthNull() {
        assertThat(evaluator.hasPermission(null, "patients", "MODULE", "VIEW")).isFalse();
    }

    @Test
    @DisplayName("hasPermission: should return false for unknown action")
    void hasPermission_shouldReturnFalse_forUnknownAction() {
        var auth = buildAuth("DOCTOR", Map.of("patients",
                new ModulePermissions(true, true, true, true, true, true)));
        assertThat(evaluator.hasPermission(auth, "patients", "MODULE", "TELEPORT")).isFalse();
    }

    // ── Helper ───────────────────────────────────────────────────────────────────

    private static OphthaClinicalAuthenticationToken buildAuth(
            String staffRole,
            Map<String, ModulePermissions> permissions) {
        OphthaPrincipal principal = new OphthaPrincipal(
                "kc-uuid",
                "tenant-111",
                "staff-222",
                staffRole,
                permissions
        );
        return new OphthaClinicalAuthenticationToken(
                principal,
                null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + staffRole))
        );
    }
}
