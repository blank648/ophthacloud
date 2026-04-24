package ro.ophthacloud.modules.emr.internal;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;

import java.util.UUID;

/**
 * Thrown when an attempt is made to modify a consultation that has already been signed (SIGNED status).
 * Maps to HTTP 409 CONSULTATION_ALREADY_SIGNED per GUIDE_04 §1.7.
 */
public class ConsultationAlreadySignedException extends DomainException {

    public ConsultationAlreadySignedException(UUID consultationId) {
        super("CONSULTATION_ALREADY_SIGNED",
                "Consultation " + consultationId + " is already signed and cannot be modified.",
                HttpStatus.CONFLICT);
    }
}
