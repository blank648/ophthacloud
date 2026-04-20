package ro.ophthacloud.modules.appointments;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;

/**
 * Thrown when a new booking overlaps an existing active appointment or blocked slot.
 * HTTP 400 DOUBLE_BOOKING per GUIDE_04 §1.7 and GUIDE_06 §2.2.
 */
public class DoubleBookingException extends DomainException {
    public DoubleBookingException(String detail) {
        super("DOUBLE_BOOKING", "Double booking detected: " + detail, HttpStatus.BAD_REQUEST);
    }
}
