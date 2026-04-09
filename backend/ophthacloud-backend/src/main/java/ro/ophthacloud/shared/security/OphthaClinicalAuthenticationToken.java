package ro.ophthacloud.shared.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/**
 * Custom {@link AbstractAuthenticationToken} that carries:
 * <ul>
 *   <li>The original Keycloak {@link Jwt} (for downstream introspection if needed)</li>
 *   <li>The fully resolved {@link OphthaPrincipal} as the principal</li>
 *   <li>The granted authorities derived from {@code staff_role} (e.g. {@code ROLE_DOCTOR})</li>
 * </ul>
 * Created exclusively by {@link OphthaClinicalJwtConverter} — never instantiated directly in business code.
 */
public class OphthaClinicalAuthenticationToken extends AbstractAuthenticationToken {

    private final OphthaPrincipal principal;
    private final Jwt jwt;

    public OphthaClinicalAuthenticationToken(
            OphthaPrincipal principal,
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt.getTokenValue();
    }

    @Override
    public OphthaPrincipal getPrincipal() {
        return principal;
    }

    /** Exposes the raw JWT for downstream use (e.g. token forwarding to Keycloak Admin API). */
    public Jwt getJwt() {
        return jwt;
    }
}
