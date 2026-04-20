package ro.ophthacloud.modules.appointments;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;
import ro.ophthacloud.shared.enums.AppointmentStatus;

/**
 * Thrown when a structural update (PUT) is attempted on an appointment whose status
 * is CHECKED_IN or beyond (IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW).
 * Only BOOKED and CONFIRMED appointments can be modified.
 * HTTP 409 APPOINTMENT_NOT_MODIFIABLE per OC-014 criterion.
 */
public class AppointmentNotModifiableException extends DomainException {
    public AppointmentNotModifiableException(AppointmentStatus currentStatus) {
        super(
            "APPOINTMENT_NOT_MODIFIABLE",
            "Appointment cannot be modified in status " + currentStatus
                + ". Only BOOKED or CONFIRMED appointments may be updated.",
            HttpStatus.CONFLICT
        );
    }
}
