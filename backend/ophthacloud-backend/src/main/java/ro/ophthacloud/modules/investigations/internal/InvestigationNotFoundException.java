package ro.ophthacloud.modules.investigations.internal;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;

import java.util.UUID;

public class InvestigationNotFoundException extends DomainException {
    public InvestigationNotFoundException(UUID id) {
        super("INVESTIGATION_NOT_FOUND", "Investigation with ID " + id + " not found.", HttpStatus.NOT_FOUND);
    }
}
