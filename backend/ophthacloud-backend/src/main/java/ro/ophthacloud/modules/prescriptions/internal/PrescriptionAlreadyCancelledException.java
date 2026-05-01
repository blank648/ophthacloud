package ro.ophthacloud.modules.prescriptions.internal;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;

public class PrescriptionAlreadyCancelledException extends DomainException {
    public PrescriptionAlreadyCancelledException() {
        super("PRESCRIPTION_ALREADY_CANCELLED",
              "This prescription has already been cancelled.", HttpStatus.CONFLICT);
    }
}
