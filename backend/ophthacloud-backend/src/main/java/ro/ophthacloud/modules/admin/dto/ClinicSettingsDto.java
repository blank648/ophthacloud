package ro.ophthacloud.modules.admin.dto;

import ro.ophthacloud.modules.admin.internal.ClinicSettingsEntity;

import java.math.BigDecimal;

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
        String prescriptionPrefix
) {
    public static ClinicSettingsDto from(ClinicSettingsEntity entity) {
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
                entity.getPrescriptionPrefix()
        );
    }
}
