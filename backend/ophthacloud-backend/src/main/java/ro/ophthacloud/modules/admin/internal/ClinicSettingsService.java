package ro.ophthacloud.modules.admin.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.admin.dto.ClinicSettingsDto;
import ro.ophthacloud.modules.admin.dto.UpdateClinicSettingsRequest;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Clinic settings service with validation per GUIDE_06 §9.3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicSettingsService {

    private static final Set<Integer> VALID_SLOT_MINUTES = Set.of(10, 15, 20, 30, 45, 60);
    private static final Set<BigDecimal> VALID_VAT_RATES = Set.of(
            BigDecimal.ZERO,
            new BigDecimal("5"),
            new BigDecimal("9"),
            new BigDecimal("19")
    );

    private final ClinicSettingsRepository clinicSettingsRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public ClinicSettingsDto getClinicSettings() {
        UUID tenantId = TenantContext.require();
        ClinicSettingsEntity entity = clinicSettingsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("Clinic settings not found for tenant " + tenantId));
        
        String name = "Clinica Oftalmologică Demo SRL";
        String cui = "";
        String phone = "";
        String email = "";
        String address = "";
        try {
            Map<String, Object> tenant = jdbcTemplate.queryForMap(
                    "SELECT name, cui, phone, email, address FROM tenants WHERE id = ?",
                    tenantId
            );
            if (tenant.get("name") != null) name = (String) tenant.get("name");
            if (tenant.get("cui") != null) cui = (String) tenant.get("cui");
            if (tenant.get("phone") != null) phone = (String) tenant.get("phone");
            if (tenant.get("email") != null) email = (String) tenant.get("email");
            if (tenant.get("address") != null) address = (String) tenant.get("address");
        } catch (Exception e) {
            log.warn("Could not fetch tenant details for tenant {}", tenantId, e);
        }

        return ClinicSettingsDto.from(entity, name, cui, phone, email, address);
    }

    @Transactional
    public ClinicSettingsDto updateClinicSettings(UpdateClinicSettingsRequest request) {
        UUID tenantId = TenantContext.require();

        // Validate domain-specific constraints per GUIDE_06 §9.3
        validate(request);

        ClinicSettingsEntity entity = clinicSettingsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("Clinic settings not found for tenant " + tenantId));

        if (request.workingHours() != null) {
            entity.setWorkingHours(request.workingHours());
        }
        if (request.bookingSlotMinutes() != null) {
            entity.setBookingSlotMinutes(request.bookingSlotMinutes());
        }
        if (request.bookingAdvanceDays() != null) {
            entity.setBookingAdvanceDays(request.bookingAdvanceDays());
        }
        if (request.vatRateDefault() != null) {
            entity.setVatRateDefault(request.vatRateDefault());
        }
        if (request.portalEnabled() != null) {
            entity.setPortalEnabled(request.portalEnabled());
        }
        if (request.portalAppointmentBooking() != null) {
            entity.setPortalAppointmentBooking(request.portalAppointmentBooking());
        }
        if (request.invoicePrefix() != null) {
            entity.setInvoicePrefix(request.invoicePrefix());
        }
        if (request.orderNumberPrefix() != null) {
            entity.setOrderNumberPrefix(request.orderNumberPrefix());
        }
        if (request.prescriptionPrefix() != null) {
            entity.setPrescriptionPrefix(request.prescriptionPrefix());
        }
        if (request.quietHoursStart() != null) {
            entity.setQuietHoursStart(request.quietHoursStart());
        }
        if (request.quietHoursEnd() != null) {
            entity.setQuietHoursEnd(request.quietHoursEnd());
        }
        if (request.maxSmsPerPatient() != null) {
            entity.setMaxSmsPerPatient(request.maxSmsPerPatient());
        }

        entity = clinicSettingsRepository.save(entity);

        if (request.name() != null) {
            jdbcTemplate.update("UPDATE tenants SET name = ? WHERE id = ?", request.name(), tenantId);
        }
        if (request.cui() != null) {
            jdbcTemplate.update("UPDATE tenants SET cui = ? WHERE id = ?", request.cui(), tenantId);
        }
        if (request.phone() != null) {
            jdbcTemplate.update("UPDATE tenants SET phone = ? WHERE id = ?", request.phone(), tenantId);
        }
        if (request.email() != null) {
            jdbcTemplate.update("UPDATE tenants SET email = ? WHERE id = ?", request.email(), tenantId);
        }
        if (request.address() != null) {
            jdbcTemplate.update("UPDATE tenants SET address = ? WHERE id = ?", request.address(), tenantId);
        }

        String name = "Clinica Oftalmologică Demo SRL";
        String cui = "";
        String phone = "";
        String email = "";
        String address = "";
        try {
            Map<String, Object> tenant = jdbcTemplate.queryForMap(
                    "SELECT name, cui, phone, email, address FROM tenants WHERE id = ?",
                    tenantId
            );
            if (tenant.get("name") != null) name = (String) tenant.get("name");
            if (tenant.get("cui") != null) cui = (String) tenant.get("cui");
            if (tenant.get("phone") != null) phone = (String) tenant.get("phone");
            if (tenant.get("email") != null) email = (String) tenant.get("email");
            if (tenant.get("address") != null) address = (String) tenant.get("address");
        } catch (Exception e) {
            log.warn("Could not fetch tenant details after update for tenant {}", tenantId, e);
        }

        log.info("Updated clinic settings for tenant {}", tenantId);
        return ClinicSettingsDto.from(entity, name, cui, phone, email, address);
    }

    /**
     * Validates domain-specific constraints from GUIDE_06 §9.3.
     * Throws {@link IllegalArgumentException} with field-level details on violation.
     */
    private void validate(UpdateClinicSettingsRequest request) {
        if (request.bookingSlotMinutes() != null && !VALID_SLOT_MINUTES.contains(request.bookingSlotMinutes())) {
            throw new IllegalArgumentException(
                    "bookingSlotMinutes must be one of " + VALID_SLOT_MINUTES + ", got: " + request.bookingSlotMinutes());
        }

        if (request.vatRateDefault() != null) {
            // Compare by numeric value, ignoring scale
            boolean validVat = VALID_VAT_RATES.stream()
                    .anyMatch(rate -> rate.compareTo(request.vatRateDefault()) == 0);
            if (!validVat) {
                throw new IllegalArgumentException(
                        "vatRateDefault must be one of {0, 5, 9, 19} (Romanian VAT rates), got: " + request.vatRateDefault());
            }
        }

        if (request.bookingAdvanceDays() != null) {
            if (request.bookingAdvanceDays() < 1 || request.bookingAdvanceDays() > 365) {
                throw new IllegalArgumentException(
                        "bookingAdvanceDays must be between 1 and 365, got: " + request.bookingAdvanceDays());
            }
        }
    }
}
