package ro.ophthacloud.modules.admin.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "clinic_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicSettingsEntity extends TenantAwareEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "working_hours", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String workingHours = "{}";

    @Builder.Default
    @Column(name = "booking_advance_days")
    private Integer bookingAdvanceDays = 90;

    @Builder.Default
    @Column(name = "booking_slot_minutes")
    private Integer bookingSlotMinutes = 15;

    @Builder.Default
    @Column(name = "invoice_prefix", length = 16)
    private String invoicePrefix = "FC";

    @Builder.Default
    @Column(name = "invoice_sequence", nullable = false)
    private Integer invoiceSequence = 0;

    @Builder.Default
    @Column(name = "order_number_prefix", length = 16)
    private String orderNumberPrefix = "CMD";

    @Builder.Default
    @Column(name = "order_number_sequence", nullable = false)
    private Integer orderNumberSequence = 0;

    @Builder.Default
    @Column(name = "prescription_prefix", length = 16)
    private String prescriptionPrefix = "RX";

    @Builder.Default
    @Column(name = "prescription_sequence", nullable = false)
    private Integer prescriptionSequence = 0;

    @Column(name = "sms_provider", length = 64)
    private String smsProvider;

    @Column(name = "sms_api_key", columnDefinition = "text")
    private String smsApiKey;

    @Column(name = "email_from", length = 256)
    private String emailFrom;

    @Column(name = "email_provider", length = 64)
    private String emailProvider;

    @Column(name = "email_api_key", columnDefinition = "text")
    private String emailApiKey;

    @Builder.Default
    @Column(name = "portal_enabled")
    private Boolean portalEnabled = true;

    @Builder.Default
    @Column(name = "portal_appointment_booking")
    private Boolean portalAppointmentBooking = false;

    @Builder.Default
    @Column(name = "currency", length = 3)
    private String currency = "RON";

    @Builder.Default
    @Column(name = "vat_rate_default", precision = 5, scale = 2)
    private BigDecimal vatRateDefault = new BigDecimal("19.00");
}
