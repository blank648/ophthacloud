package ro.ophthacloud.shared.api;

import org.springframework.http.HttpStatus;

/**
 * Base class for all domain-specific application exceptions.
 * Allows the {@link GlobalExceptionHandler} in the shared module
 * to catch business exceptions without depending on other modules,
 * satisfying Spring Modulith's strict acyclic dependency rules.
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public DomainException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
