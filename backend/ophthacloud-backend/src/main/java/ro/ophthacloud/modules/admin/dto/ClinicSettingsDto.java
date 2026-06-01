package ro.ophthacloud.modules.admin.dto;

import ro.ophthacloud.modules.admin.internal.ClinicSettingsEntity;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * DTO for clinic settings response.
 * Matches GUIDE_04 §10.3 GET response shape.
 */
public record ClinicSettingsDto(
        String workingHours,
        int bookingSlotMinutes,
        int bookingAdvanceDays,
        String currency,
        BigDecimal vatRateDefault,
        boolean portalEnabled,
        boolean portalAppointmentBooking,
        String invoicePrefix,
        String orderNumberPrefix,
        String prescriptionPrefix,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd,
        Integer maxSmsPerPatient,
        String name,
        String cui,
        String phone,
        String email,
        String address
) {
    public static ClinicSettingsDto from(ClinicSettingsEntity entity, String name, String cui, String phone, String email, String address) {
        return new ClinicSettingsDto(
                entity.getWorkingHours(),
                entity.getBookingSlotMinutes(),
                entity.getBookingAdvanceDays(),
                entity.getCurrency(),
                entity.getVatRateDefault(),
                entity.getPortalEnabled(),
                entity.getPortalAppointmentBooking(),
                entity.getInvoicePrefix(),
                entity.getOrderNumberPrefix(),
                entity.getPrescriptionPrefix(),
                entity.getQuietHoursStart(),
                entity.getQuietHoursEnd(),
                entity.getMaxSmsPerPatient(),
                name,
                cui,
                phone,
                email,
                address
        );
    }
}
