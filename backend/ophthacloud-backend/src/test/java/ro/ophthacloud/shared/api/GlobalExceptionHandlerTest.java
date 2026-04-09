package ro.ophthacloud.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(request.getHeader("X-Request-Id")).thenReturn(UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("handleValidation: should return 400 VALIDATION_ERROR with field details")
    void handleValidation_shouldReturnValidationError_withFieldDetails() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "firstName", null, false,
                new String[]{"NotBlank"}, null, "First name is required."));
        bindingResult.addError(new FieldError("target", "dateOfBirth", null, false,
                new String[]{"Past"}, null, "Date of birth must be in the past."));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                (MethodParameter) null, bindingResult);

        ErrorResponse response = handler.handleValidation(ex, request);

        assertThat(response.error().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.error().details()).hasSize(2);
        assertThat(response.error().details().get(0).field()).isEqualTo("firstName");
        assertThat(response.error().details().get(0).code()).isEqualTo("REQUIRED");
        assertThat(response.error().details().get(1).field()).isEqualTo("dateOfBirth");
        assertThat(response.error().details().get(1).code()).isEqualTo("INVALID_DATE");
    }

    @Test
    @DisplayName("handleValidation: should include path and requestId in error body")
    void handleValidation_shouldIncludePath_andRequestId() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                (MethodParameter) null, bindingResult);

        ErrorResponse response = handler.handleValidation(ex, request);

        assertThat(response.error().path()).isEqualTo("/api/v1/test");
        assertThat(response.error().requestId()).isNotBlank();
        assertThat(response.error().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleAccessDenied: should return 403 FORBIDDEN")
    void handleAccessDenied_shouldReturnForbidden() {
        var ex = new org.springframework.security.access.AccessDeniedException("denied");

        ErrorResponse response = handler.handleAccessDenied(ex, request);

        assertThat(response.error().code()).isEqualTo("FORBIDDEN");
        assertThat(response.error().details()).isNull();
    }

    @Test
    @DisplayName("handleUnexpected: should return 500 INTERNAL_ERROR")
    void handleUnexpected_shouldReturnInternalError() {
        var ex = new RuntimeException("Something went wrong");

        ErrorResponse response = handler.handleUnexpected(ex, request);

        assertThat(response.error().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.error().message()).isEqualTo("An unexpected error occurred.");
    }

    @Test
    @DisplayName("handleIllegalArgument: should return 400 BAD_REQUEST")
    void handleIllegalArgument_shouldReturnBadRequest() {
        var ex = new IllegalArgumentException("Invalid input value");

        ErrorResponse response = handler.handleIllegalArgument(ex, request);

        assertThat(response.error().code()).isEqualTo("BAD_REQUEST");
        assertThat(response.error().message()).isEqualTo("Invalid input value");
    }

    @Test
    @DisplayName("requestId: should use X-Request-Id header when present")
    void requestId_shouldUseHeader_whenPresent() throws Exception {
        String customId = "custom-request-id-123";
        when(request.getHeader("X-Request-Id")).thenReturn(customId);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "t");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                (MethodParameter) null, bindingResult);

        ErrorResponse response = handler.handleValidation(ex, request);
        assertThat(response.error().requestId()).isEqualTo(customId);
    }

    @Test
    @DisplayName("requestId: should generate UUID when X-Request-Id header is absent")
    void requestId_shouldGenerateUuid_whenHeaderAbsent() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "t");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                (MethodParameter) null, bindingResult);

        ErrorResponse response = handler.handleValidation(ex, request);
        assertThat(response.error().requestId()).isNotBlank();
    }
}
