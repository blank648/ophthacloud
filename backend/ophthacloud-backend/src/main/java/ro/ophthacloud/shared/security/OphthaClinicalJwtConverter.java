package ro.ophthacloud.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts an incoming Keycloak {@link Jwt} into an {@link OphthaClinicalAuthenticationToken}.
 * <p>
 * Registered as the JWT authentication converter in {@link SecurityConfig}.
 * Per GUIDE_05 §4.2:
 * <ol>
 *   <li>Reads {@code tenant_id}, {@code staff_id}, {@code staff_role}, {@code permissions} from claims</li>
 *   <li>Throws {@link InvalidBearerTokenException} if {@code tenant_id} is missing — hard security rule</li>
 *   <li>Builds {@link OphthaPrincipal} + {@link OphthaClinicalAuthenticationToken}</li>
 * </ol>
 */
@Component
@Slf4j
public class OphthaClinicalJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String tenantId  = jwt.getClaimAsString("tenant_id");
        String staffId   = jwt.getClaimAsString("staff_id");
        String staffRole = jwt.getClaimAsString("staff_role");

        // GUIDE_05 §4.2 — mandatory claim: any token without tenant_id is rejected
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("JWT missing tenant_id claim for subject={}", jwt.getSubject());
            throw new InvalidBearerTokenException("Token does not contain required tenant_id claim.");
        }

        Map<String, ModulePermissions> permissions = parsePermissions(jwt);

        OphthaPrincipal principal = new OphthaPrincipal(
                jwt.getSubject(),
                tenantId,
                staffId,
                staffRole,
                permissions
        );

        Collection<GrantedAuthority> authorities = buildAuthorities(staffRole);

        log.debug("JWT converted: subject={}, tenantId={}, staffRole={}", jwt.getSubject(), tenantId, staffRole);
        return new OphthaClinicalAuthenticationToken(principal, jwt, authorities);
    }

    // ── Permissions parsing ──────────────────────────────────────────────────────

    /**
     * Parses the {@code permissions} claim from the JWT.
     * <p>
     * Expected JWT shape (GUIDE_05 §3.1):
     * <pre>
     * "permissions": {
     *   "patients": { "view": true, "create": true, "edit": true, "delete": false, "sign": false, "export": true },
     *   ...
     * }
     * </pre>
     * If the claim is absent or malformed, returns an empty map (deny-all for all modules).
     */
    private Map<String, ModulePermissions> parsePermissions(Jwt jwt) {
        Object raw = jwt.getClaims().get("permissions");
        if (raw == null) {
            log.debug("JWT has no permissions claim for subject={} — using deny-all defaults", jwt.getSubject());
            return Collections.emptyMap();
        }

        if (!(raw instanceof Map<?, ?> rawMap)) {
            log.warn("JWT permissions claim is not a Map for subject={}", jwt.getSubject());
            return Collections.emptyMap();
        }

        Map<String, ModulePermissions> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            String moduleCode = String.valueOf(entry.getKey());
            if (!(entry.getValue() instanceof Map<?, ?> permMap)) continue;

            result.put(moduleCode, new ModulePermissions(
                    getBool(permMap, "view"),
                    getBool(permMap, "create"),
                    getBool(permMap, "edit"),
                    getBool(permMap, "delete"),
                    getBool(permMap, "sign"),
                    getBool(permMap, "export")
            ));
        }
        return Collections.unmodifiableMap(result);
    }

    private boolean getBool(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return Boolean.TRUE.equals(val);
    }

    private Collection<GrantedAuthority> buildAuthorities(String staffRole) {
        if (staffRole == null || staffRole.isBlank()) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + staffRole));
    }
}
