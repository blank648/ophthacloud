package ro.ophthacloud.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Error response envelope for all 4xx and 5xx responses.
 * <p>
 * Shape matches GUIDE_04 §1.7:
 * <pre>
 * {
 *   "error": {
 *     "code": "PATIENT_NOT_FOUND",
 *     "message": "Patient with ID abc-123 not found.",
 *     "timestamp": "2026-04-02T09:30:00Z",
 *     "path": "/api/v1/patients/abc-123",
 *     "requestId": "req-uuid-here",
 *     "details": [...]   // only present on VALIDATION_ERROR
 *   }
 * }
 * </pre>
 * The outer wrapper exists so Jackson produces {@code { "error": { ... } }} rather than a flat object.
 */
public record ErrorResponse(ErrorBody error) {

    /**
     * Inner body record.
     *
     * @param code      machine-readable error code (e.g. {@code PATIENT_NOT_FOUND})
     * @param message   human-readable description
     * @param timestamp UTC instant when the error occurred
     * @param path      request URI that triggered the error
     * @param requestId correlation ID for log tracing (from {@code X-Request-Id} header, or generated)
     * @param details   field-level validation errors; {@code null} unless {@code code = VALIDATION_ERROR}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(
            String code,
            String message,
            Instant timestamp,
            String path,
            String requestId,
            List<FieldError> details
    ) {}

    /**
     * Field-level validation error included in {@code details} for {@code VALIDATION_ERROR} responses.
     *
     * @param field   the camelCase field name that failed validation
     * @param code    machine-readable subcode (e.g. {@code REQUIRED}, {@code INVALID_FORMAT}, {@code SIZE})
     * @param message human-readable message for this field
     */
    public record FieldError(String field, String code, String message) {}

    // ── Factory methods ─────────────────────────────────────────────────────────

    /** Creates a simple (non-validation) error response. */
    public static ErrorResponse of(String code, String message, String path, String requestId) {
        return new ErrorResponse(new ErrorBody(code, message, Instant.now(), path, requestId, null));
    }

    /** Creates a validation error response with field-level details. */
    public static ErrorResponse validation(List<FieldError> details, String path, String requestId) {
        return new ErrorResponse(new ErrorBody(
                "VALIDATION_ERROR",
                "Request body contains invalid fields.",
                Instant.now(),
                path,
                requestId,
                details
        ));
    }
}
