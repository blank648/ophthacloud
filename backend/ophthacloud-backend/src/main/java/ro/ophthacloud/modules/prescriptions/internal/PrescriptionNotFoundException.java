package ro.ophthacloud.modules.prescriptions.internal;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;

import java.util.UUID;

public class PrescriptionNotFoundException extends DomainException {
    public PrescriptionNotFoundException(UUID id) {
        super("PRESCRIPTION_NOT_FOUND", "Prescription with ID " + id + " not found.", HttpStatus.NOT_FOUND);
    }
}
