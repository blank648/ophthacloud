package ro.unibuc.fmi.ophthacloud.securityaudit.service;

import ro.unibuc.fmi.ophthacloud.securityaudit.api.AuthenticatedUser;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.CurrentUserProvider;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class SecurityContextCurrentUserProvider implements CurrentUserProvider {
    @Override
    public AuthenticatedUser currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new AuthenticationCredentialsNotFoundException("Authentication required");
    }
}
