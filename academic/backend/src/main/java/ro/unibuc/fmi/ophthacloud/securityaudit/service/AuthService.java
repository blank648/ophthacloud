package ro.unibuc.fmi.ophthacloud.securityaudit.service;

import java.util.Locale;
import ro.unibuc.fmi.ophthacloud.securityaudit.dto.LoginRequest;
import ro.unibuc.fmi.ophthacloud.securityaudit.dto.LoginResponse;
import ro.unibuc.fmi.ophthacloud.securityaudit.entity.UserEntity;
import ro.unibuc.fmi.ophthacloud.securityaudit.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtTokenService tokens;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtTokenService tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.dummyHash = passwords.encode("unknown-account");
    }

    public LoginResponse login(LoginRequest request) {
        var account = users.findByEmail(request.email().trim().toLowerCase(Locale.ROOT));
        boolean matches = passwords.matches(request.password(), account.map(UserEntity::getPasswordHash).orElse(dummyHash));
        if (!matches || account.isEmpty() || !account.get().isActive()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        var user = account.get().identity();
        return new LoginResponse(tokens.issue(user), "Bearer", tokens.expiresInSeconds(), user.role());
    }
}
