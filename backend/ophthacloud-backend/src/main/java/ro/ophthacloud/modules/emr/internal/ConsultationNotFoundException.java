package ro.ophthacloud.modules.emr.internal;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;

import java.util.UUID;

/**
 * Thrown when a consultation ID is not found in the current tenant's context.
 * Maps to HTTP 404 CONSULTATION_NOT_FOUND per GUIDE_04 §1.7.
 */
public class ConsultationNotFoundException extends DomainException {

    public ConsultationNotFoundException(UUID id) {
        super("CONSULTATION_NOT_FOUND",
                "Consultation not found: " + id,
                HttpStatus.NOT_FOUND);
    }
}
