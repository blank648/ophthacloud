package ro.unibuc.fmi.ophthacloud.securityaudit.config;

import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.util.*;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.*;
import ro.unibuc.fmi.ophthacloud.securityaudit.entity.UserEntity;
import ro.unibuc.fmi.ophthacloud.securityaudit.repository.UserRepository;
import ro.unibuc.fmi.ophthacloud.securityaudit.service.JwtTokenService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final UserRepository users = mock(UserRepository.class);
    private final JwtTokenService tokens = new JwtTokenService("k".repeat(32), 15, Clock.systemUTC());
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens, users);
    private final AuthenticatedUser patient = new AuthenticatedUser(UUID.randomUUID(), "p@example.com", UserRole.PATIENT);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesActiveAccountWithCurrentAuthorities() throws Exception {
        var account = mock(UserEntity.class);
        when(account.identity()).thenReturn(patient);
        when(users.findByIdAndActiveTrue(patient.userId())).thenReturn(Optional.of(account));
        assertThat(protectedRequest("Bearer " + tokens.issue(patient))).isEqualTo(200);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo(patient);
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_PATIENT");
    }

    @Test
    void deactivationTakesEffectOnNextRequestWithSameToken() throws Exception {
        var account = mock(UserEntity.class);
        when(account.identity()).thenReturn(patient);
        when(users.findByIdAndActiveTrue(patient.userId())).thenReturn(Optional.of(account)).thenReturn(Optional.empty());
        String authorization = "Bearer " + tokens.issue(patient);
        assertThat(protectedRequest(authorization)).isEqualTo(200);
        clearContext();
        assertThat(protectedRequest(authorization)).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic abc", "Bearer", "Bearer bad.jwt"})
    void missingOrMalformedHeaderLeavesRequestUnauthenticated(String header) throws Exception {
        assertThat(protectedRequest(header)).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(users);
    }

    private int protectedRequest(String authorization) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/me");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        var response = new MockHttpServletResponse();
        var errors = new ExceptionTranslationFilter((req, res, ex) -> res.setStatus(401), new NullRequestCache());
        var protect = new AuthorizationFilter(AuthenticatedAuthorizationManager.authenticated());
        FilterChain endpoint = new MockFilterChain();
        filter.doFilter(request, response, (req, res) -> errors.doFilter(req, res,
                (secured, output) -> protect.doFilter(secured, output, endpoint)));
        return response.getStatus();
    }
}
