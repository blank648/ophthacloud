package ro.ophthacloud.modules.portal;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;

/**
 * Thrown when a portal patient attempts to access a resource that doesn't belong to them.
 * Results in an HTTP 403 Forbidden response.
 */
public class PortalAccessDeniedException extends DomainException {

    public PortalAccessDeniedException(String message) {
        super("PORTAL_ACCESS_DENIED", message, HttpStatus.FORBIDDEN);
    }
}
