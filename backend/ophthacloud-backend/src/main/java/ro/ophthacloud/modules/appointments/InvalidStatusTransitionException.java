package ro.ophthacloud.modules.appointments;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;
import ro.ophthacloud.shared.enums.AppointmentStatus;

/**
 * Thrown when an invalid state machine transition is attempted.
 * e.g. COMPLETED → CHECKED_IN. HTTP 400 INVALID_STATUS_TRANSITION.
 */
public class InvalidStatusTransitionException extends DomainException {
    public InvalidStatusTransitionException(AppointmentStatus from, AppointmentStatus to) {
        super("INVALID_STATUS_TRANSITION", "Cannot transition appointment from " + from + " to " + to + ".", HttpStatus.BAD_REQUEST);
    }
}
