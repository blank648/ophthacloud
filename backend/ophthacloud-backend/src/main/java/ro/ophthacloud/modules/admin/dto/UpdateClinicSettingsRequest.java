package ro.ophthacloud.modules.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * PUT request body for updating clinic settings.
 * Validation rules per GUIDE_06 §9.3.
 */
public record UpdateClinicSettingsRequest(
        String workingHours,
        Integer bookingSlotMinutes,
        @Min(1) @Max(365) Integer bookingAdvanceDays,
        BigDecimal vatRateDefault,
        Boolean portalEnabled,
        Boolean portalAppointmentBooking,
        @Size(max = 5) @Pattern(regexp = "^[A-Za-z0-9]*$", message = "Prefix must be alphanumeric, max 5 chars")
        String invoicePrefix,
        @Size(max = 5) @Pattern(regexp = "^[A-Za-z0-9]*$", message = "Prefix must be alphanumeric, max 5 chars")
        String orderNumberPrefix,
        @Size(max = 5) @Pattern(regexp = "^[A-Za-z0-9]*$", message = "Prefix must be alphanumeric, max 5 chars")
        String prescriptionPrefix,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd,
        @Min(0) Integer maxSmsPerPatient,
        String name,
        String cui,
        String phone,
        String email,
        String address
) {
}
