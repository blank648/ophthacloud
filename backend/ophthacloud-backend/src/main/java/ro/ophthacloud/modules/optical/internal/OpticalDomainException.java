package ro.ophthacloud.modules.optical.internal;

import org.springframework.http.HttpStatus;
import ro.ophthacloud.shared.api.DomainException;

public class OpticalDomainException extends DomainException {
    public OpticalDomainException(String errorCode, String message, HttpStatus status) {
        super(errorCode, message, status);
    }
}
