package ro.ophthacloud.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

/**
 * Centralised exception → HTTP response mapper for all modules.
 * <p>
 * Every caught exception is translated into an {@link ErrorResponse} matching GUIDE_04 §1.7.
 * New domain exceptions from business modules must be registered here with their error code and status.
 * <p>
 * Order matters for overlapping exception hierarchies — most specific handlers first.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 400 Bad Request ──────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        BindingResult result = ex.getBindingResult();
        List<ErrorResponse.FieldError> fieldErrors = result.getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(
                        fe.getField(),
                        resolveValidationCode(fe.getCode()),
                        fe.getDefaultMessage()))
                .toList();
        log.debug("Validation failed on {}: {} field errors", req.getRequestURI(), fieldErrors.size());
        return ErrorResponse.validation(fieldErrors, req.getRequestURI(), requestId(req));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String message = "Parameter '%s' has invalid value: '%s'".formatted(ex.getName(), ex.getValue());
        log.debug("Type mismatch on {}: {}", req.getRequestURI(), message);
        return ErrorResponse.of("INVALID_PARAMETER", message, req.getRequestURI(), requestId(req));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        log.debug("Illegal argument on {}: {}", req.getRequestURI(), ex.getMessage());
        return ErrorResponse.of("BAD_REQUEST", ex.getMessage(), req.getRequestURI(), requestId(req));
    }

    // ── 401 Unauthorized ─────────────────────────────────────────────────────────

    @ExceptionHandler(InvalidBearerTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidToken(InvalidBearerTokenException ex, HttpServletRequest req) {
        log.debug("Invalid bearer token on {}: {}", req.getRequestURI(), ex.getMessage());
        return ErrorResponse.of("UNAUTHORIZED", "Missing or invalid authentication token.",
                req.getRequestURI(), requestId(req));
    }

    // ── 403 Forbidden ─────────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.debug("Access denied on {}: {}", req.getRequestURI(), ex.getMessage());
        return ErrorResponse.of("FORBIDDEN", "You do not have permission to perform this action.",
                req.getRequestURI(), requestId(req));
    }

    // ── Domain Errors ────────────────────────────────────────────────────────

    @ExceptionHandler(DomainException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleDomainException(
            DomainException ex,
            HttpServletRequest req) {
        log.debug("Domain error on {}: {}", req.getRequestURI(), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), req.getRequestURI(), requestId(req));
        return org.springframework.http.ResponseEntity.status(ex.getStatus()).body(response);
    }

    // ── 409 Conflict ─────────────────────────────────────────────────────────────
    // ConsultationAlreadySignedException registered in EMR sprint.

    // ── 500 Internal Server Error ────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {} [requestId={}]", req.getRequestURI(), requestId(req), ex);
        return ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred.",
                req.getRequestURI(), requestId(req));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private String requestId(HttpServletRequest req) {
        String header = req.getHeader("X-Request-Id");
        return (header != null && !header.isBlank()) ? header : UUID.randomUUID().toString();
    }

    private String resolveValidationCode(String constraintCode) {
        if (constraintCode == null) return "INVALID";
        return switch (constraintCode) {
            case "NotNull", "NotBlank", "NotEmpty" -> "REQUIRED";
            case "Size", "Min", "Max", "DecimalMin", "DecimalMax" -> "SIZE";
            case "Past", "Future", "PastOrPresent", "FutureOrPresent" -> "INVALID_DATE";
            case "Email" -> "INVALID_FORMAT";
            case "Pattern" -> "INVALID_FORMAT";
            default -> "INVALID_FORMAT";
        };
    }
}
