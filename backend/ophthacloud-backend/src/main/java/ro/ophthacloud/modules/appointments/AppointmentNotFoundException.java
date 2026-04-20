package ro.ophthacloud.modules.appointments;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;

/**
 * Thrown when an appointment with the given ID does not exist within the current tenant.
 * Mapped to HTTP 404 APPOINTMENT_NOT_FOUND by {@link ro.ophthacloud.shared.api.GlobalExceptionHandler}.
 */
public class AppointmentNotFoundException extends DomainException {

    public AppointmentNotFoundException(UUID appointmentId) {
        super("APPOINTMENT_NOT_FOUND", "Appointment with ID " + appointmentId + " not found.", HttpStatus.NOT_FOUND);
    }
}
