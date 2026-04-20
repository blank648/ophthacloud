package ro.ophthacloud.modules.patients;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;

/**
 * Thrown when a patient with the given ID does not exist within the current tenant.
 * Mapped to HTTP 404 PATIENT_NOT_FOUND by {@link ro.ophthacloud.shared.api.GlobalExceptionHandler}.
 */
public class PatientNotFoundException extends DomainException {

    public PatientNotFoundException(UUID patientId) {
        super("PATIENT_NOT_FOUND", "Patient with ID " + patientId + " not found.", HttpStatus.NOT_FOUND);
    }
}
