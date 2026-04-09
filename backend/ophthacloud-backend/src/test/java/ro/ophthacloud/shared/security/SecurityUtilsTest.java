package ro.ophthacloud.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SecurityUtils")
class SecurityUtilsTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("currentTenantId: should return tenantId from OphthaPrincipal in context")
    void currentTenantId_shouldReturnTenantId_whenAuthenticated() {
        setUpSecurityContext("tenant-aaa", "staff-bbb", "DOCTOR");
        assertThat(SecurityUtils.currentTenantId()).isEqualTo("tenant-aaa");
    }

    @Test
    @DisplayName("currentStaffId: should return staffId from OphthaPrincipal in context")
    void currentStaffId_shouldReturnStaffId_whenAuthenticated() {
        setUpSecurityContext("tenant-aaa", "staff-bbb", "NURSE");
        assertThat(SecurityUtils.currentStaffId()).isEqualTo("staff-bbb");
    }

    @Test
    @DisplayName("currentStaffRole: should return staffRole from OphthaPrincipal in context")
    void currentStaffRole_shouldReturnRole_whenAuthenticated() {
        setUpSecurityContext("tenant-xxx", "staff-yyy", "CLINIC_ADMIN");
        assertThat(SecurityUtils.currentStaffRole()).isEqualTo("CLINIC_ADMIN");
    }

    @Test
    @DisplayName("currentTenantId: should throw IllegalStateException when context is empty")
    void currentTenantId_shouldThrow_whenContextEmpty() {
        assertThatThrownBy(SecurityUtils::currentTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OphthaPrincipal");
    }

    @Test
    @DisplayName("isAuthenticated: should return true when OphthaPrincipal is in context")
    void isAuthenticated_shouldReturnTrue_whenPrincipalPresent() {
        setUpSecurityContext("tenant-aaa", "staff-bbb", "DOCTOR");
        assertThat(SecurityUtils.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("isAuthenticated: should return false when context is empty")
    void isAuthenticated_shouldReturnFalse_whenContextEmpty() {
        assertThat(SecurityUtils.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("currentPrincipal: should return the exact OphthaPrincipal stored in context")
    void currentPrincipal_shouldReturnPrincipal_whenAuthenticated() {
        setUpSecurityContext("tenant-xyz", "staff-xyz", "OPTOMETRIST");
        OphthaPrincipal principal = SecurityUtils.currentPrincipal();
        assertThat(principal.tenantId()).isEqualTo("tenant-xyz");
        assertThat(principal.staffRole()).isEqualTo("OPTOMETRIST");
    }

    // ── Helper ───────────────────────────────────────────────────────────────────

    private static void setUpSecurityContext(String tenantId, String staffId, String staffRole) {
        OphthaPrincipal principal = new OphthaPrincipal(
                "kc-sub-uuid",
                tenantId,
                staffId,
                staffRole,
                Map.of()
        );
        OphthaClinicalAuthenticationToken auth = new OphthaClinicalAuthenticationToken(
                principal,
                null, // jwt not needed for SecurityUtils tests
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + staffRole))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
