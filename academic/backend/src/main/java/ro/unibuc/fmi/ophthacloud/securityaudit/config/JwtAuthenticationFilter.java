package ro.unibuc.fmi.ophthacloud.securityaudit.config;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.AuthenticatedUser;
import ro.unibuc.fmi.ophthacloud.securityaudit.entity.UserEntity;
import ro.unibuc.fmi.ophthacloud.securityaudit.repository.UserRepository;
import ro.unibuc.fmi.ophthacloud.securityaudit.service.JwtTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService tokens;
    private final UserRepository users;

    JwtAuthenticationFilter(JwtTokenService tokens, UserRepository users) {
        this.tokens = tokens;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                var identity = tokens.parse(header.substring(7));
                users.findByIdAndActiveTrue(identity.userId()).map(UserEntity::identity)
                        .filter(user -> user.role() == identity.role()).ifPresent(this::authenticate);
            } catch (JwtException | IllegalArgumentException invalidToken) {
                SecurityContextHolder.clearContext();
            }
        }
        // Anonymous requests to protected routes receive the chain's JSON authentication entry point.
        chain.doFilter(request, response);
    }

    private void authenticate(AuthenticatedUser user) {
        var authority = new SimpleGrantedAuthority("ROLE_" + user.role().name());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of(authority)));
        SecurityContextHolder.setContext(context);
    }
}
