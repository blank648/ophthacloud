package ro.unibuc.fmi.ophthacloud.securityaudit.service;

import java.util.Optional;
import java.util.UUID;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.*;
import ro.unibuc.fmi.ophthacloud.securityaudit.dto.*;
import ro.unibuc.fmi.ophthacloud.securityaudit.entity.UserEntity;
import ro.unibuc.fmi.ophthacloud.securityaudit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    private final JwtTokenService tokens = mock(JwtTokenService.class);
    private final AuthService service = new AuthService(users, passwords, tokens);
    private final AuthenticatedUser identity = new AuthenticatedUser(UUID.randomUUID(), "user@example.com", UserRole.DOCTOR);

    private void account(boolean active) {
        var account = mock(UserEntity.class);
        when(account.getPasswordHash()).thenReturn(passwords.encode("correct"));
        when(account.isActive()).thenReturn(active);
        when(account.identity()).thenReturn(identity);
        when(users.findByEmail(identity.email())).thenReturn(Optional.of(account));
    }

    @Test
    void normalizesEmailBeforeIssuingToken() {
        account(true);
        when(tokens.issue(identity)).thenReturn("access");
        when(tokens.expiresInSeconds()).thenReturn(900L);
        var result = service.login(new LoginRequest("  USER@Example.COM  ", "correct"));
        assertThat(result).isEqualTo(new LoginResponse("access", "Bearer", 900, UserRole.DOCTOR));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "inactive", "wrong-password"})
    void refusesLoginWithSameAuthenticationError(String reason) {
        if (!reason.equals("unknown")) {
            account(!reason.equals("inactive"));
        }
        String password = reason.equals("wrong-password") ? "wrong" : "correct";
        assertThatThrownBy(() -> service.login(new LoginRequest(identity.email(), password)))
                .isInstanceOf(BadCredentialsException.class).hasMessage("Invalid credentials");
        verifyNoInteractions(tokens);
    }
}
