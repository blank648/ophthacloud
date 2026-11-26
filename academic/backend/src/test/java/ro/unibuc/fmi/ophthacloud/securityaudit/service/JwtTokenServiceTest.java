package ro.unibuc.fmi.ophthacloud.securityaudit.service;

import io.jsonwebtoken.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class JwtTokenServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC);
    private final String secret = "a".repeat(64);
    private final SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private final JwtTokenService tokens = new JwtTokenService(secret, 15, clock);
    private final AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "test@example.com", UserRole.PATIENT);

    @Test
    void issuesOnlyExpectedClaimsAndRoundTripsIdentity() {
        String jwt = tokens.issue(user);
        var signed = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build().parseSignedClaims(jwt);
        assertThat(signed.getHeader().getAlgorithm()).isEqualTo("HS256");
        var claims = signed.getPayload();
        assertThat(claims.keySet()).containsExactlyInAnyOrder("iss", "aud", "exp", "iat", "sub", "role");
        assertThat(claims.getExpiration().toInstant()).isEqualTo(clock.instant().plusSeconds(900));
        assertThat(tokens.parse(jwt)).isEqualTo(new JwtTokenService.Identity(user.userId(), user.role()));
    }

    @Test
    void rejectsWrongSignature() {
        var other = new JwtTokenService("b".repeat(64), 15, clock);
        assertThatThrownBy(() -> tokens.parse(other.issue(user))).isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"issuer", "audience", "expired", "expires-now", "missing-exp", "algorithm"})
    void rejectsInvalidSignedClaimsOrAlgorithm(String defect) {
        var builder = Jwts.builder()
                .issuer(defect.equals("issuer") ? "other" : "ophthacloud-academic")
                .audience().add(defect.equals("audience") ? "other" : "ophthacloud-web").and()
                .subject(user.userId().toString()).claim("role", user.role().name());
        if (!defect.equals("missing-exp")) {
            long offset = switch (defect) {
                case "expired" -> -1;
                case "expires-now" -> 0;
                default -> 900;
            };
            builder.expiration(Date.from(clock.instant().plusSeconds(offset)));
        }
        String jwt = defect.equals("algorithm")
                ? builder.signWith(new SecretKeySpec(key.getEncoded(), "HmacSHA512"), Jwts.SIG.HS512).compact()
                : builder.signWith(key, Jwts.SIG.HS256).compact();
        assertThatThrownBy(() -> tokens.parse(jwt)).isInstanceOf(JwtException.class);
    }
}
