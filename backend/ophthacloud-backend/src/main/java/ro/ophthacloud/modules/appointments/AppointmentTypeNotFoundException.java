package ro.ophthacloud.modules.appointments;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;
import java.util.UUID;

/** Thrown when an appointment type is not found within the current tenant. HTTP 404. */
public class AppointmentTypeNotFoundException extends DomainException {
    public AppointmentTypeNotFoundException(UUID id) {
        super("APPOINTMENT_TYPE_NOT_FOUND", "Appointment type with ID " + id + " not found.", HttpStatus.NOT_FOUND);
    }
}
