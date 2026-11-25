package ro.unibuc.fmi.ophthacloud.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsAllStandardApiErrorStatusesWithRequestId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "req-stage0-test");

        ResponseEntity<ApiErrorResponse> badRequest = handler.handleBadRequest(
                new HttpMessageNotReadableException("bad json", new MockHttpInputMessage("{}".getBytes(StandardCharsets.UTF_8))),
                request
        );
        ResponseEntity<ApiErrorResponse> unauthorized = handler.handleUnauthorized(
                new BadCredentialsException("invalid"),
                request
        );
        ResponseEntity<ApiErrorResponse> forbidden = handler.handleForbidden(
                new AccessDeniedException("denied"),
                request
        );
        ResponseEntity<ApiErrorResponse> notFound = handler.handleNotFound(
                new ResourceNotFoundException("PATIENT_NOT_FOUND", "Patient not found"),
                request
        );
        ResponseEntity<ApiErrorResponse> conflict = handler.handleConflict(
                new ConflictException("APPOINTMENT_OVERLAP", "Overlapping slot"),
                request
        );
        ResponseEntity<ApiErrorResponse> unprocessable = handler.handleBusinessRule(
                new BusinessRuleException("DOSE_WINDOW_CLOSED", "Dose window closed"),
                request
        );

        assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badRequest.getBody()).isNotNull();
        assertThat(badRequest.getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(badRequest.getBody().requestId()).isEqualTo("req-stage0-test");

        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthorized.getBody().code()).isEqualTo("UNAUTHORIZED");

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbidden.getBody().code()).isEqualTo("FORBIDDEN");

        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFound.getBody().code()).isEqualTo("PATIENT_NOT_FOUND");

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody().code()).isEqualTo("APPOINTMENT_OVERLAP");

        assertThat(unprocessable.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(unprocessable.getBody().code()).isEqualTo("DOSE_WINDOW_CLOSED");
    }
}
