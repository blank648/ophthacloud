package ro.unibuc.fmi.ophthacloud.securityaudit.service;

import io.jsonwebtoken.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.AuthenticatedUser;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private static final String ISSUER = "ophthacloud-academic";
    private static final String AUDIENCE = "ophthacloud-web";
    private final SecretKey key;
    private final Clock clock;
    private final Duration lifetime;
    private final JwtParser parser;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret,
                           @Value("${app.jwt.expiration-minutes}") long minutes, Clock clock) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32 || minutes < 1) {
            throw new IllegalArgumentException("JWT requires a 256-bit minimum key and positive lifetime");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
        this.clock = clock;
        this.lifetime = Duration.ofMinutes(minutes);
        this.parser = Jwts.parser().keyLocator(header -> {
                    if (!Jwts.SIG.HS256.getId().equals(header.getAlgorithm())) {
                        throw new UnsupportedJwtException("Unsupported signing algorithm");
                    }
                    return key;
                })
                .clock(() -> Date.from(clock.instant()))
                .requireIssuer(ISSUER).requireAudience(AUDIENCE).build();
    }

    public long expiresInSeconds() {
        return lifetime.toSeconds();
    }

    public String issue(AuthenticatedUser user) {
        var now = clock.instant();
        return Jwts.builder().issuer(ISSUER).audience().add(AUDIENCE).and()
                .subject(user.userId().toString()).claim("role", user.role().name())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(lifetime)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public Identity parse(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        if (claims.getExpiration() == null || !claims.getExpiration().toInstant().isAfter(clock.instant())
                || claims.getSubject() == null || claims.get("role") == null) {
            throw new MalformedJwtException("Missing or expired identity");
        }
        return new Identity(UUID.fromString(claims.getSubject()), UserRole.valueOf(claims.get("role", String.class)));
    }

    public record Identity(UUID userId, UserRole role) {}
}
