package ro.ophthacloud.modules.notifications.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ro.ophthacloud.modules.appointments.event.AppointmentBookedEvent;
import ro.ophthacloud.modules.appointments.event.AppointmentStatusChangedEvent;
import ro.ophthacloud.modules.notifications.internal.NotificationRuleEngine;
import ro.ophthacloud.modules.patients.PatientManagementFacade;
import ro.ophthacloud.modules.patients.dto.PatientDto;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Listens to appointment-related Spring Modulith events and triggers notifications.
 * <p>
 * Handles:
 * <ul>
 *   <li>{@link AppointmentBookedEvent}         → trigger {@code APPOINTMENT_CONFIRMED}</li>
 *   <li>{@link AppointmentStatusChangedEvent}  → trigger {@code APPOINTMENT_NO_SHOW} (when status = NO_SHOW)</li>
 *   <li>{@link AppointmentStatusChangedEvent}  → trigger {@code APPOINTMENT_CONFIRMED} (when status = CONFIRMED)</li>
 * </ul>
 * Per GUIDE_06 §7.2.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventListener {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                    .withLocale(Locale.forLanguageTag("ro-RO"));
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationRuleEngine    ruleEngine;
    private final PatientManagementFacade   patientManagementFacade;

    /**
     * Handles appointment creation. Sends a booking confirmation (APPOINTMENT_CONFIRMED trigger).
     */
    @ApplicationModuleListener
    public void onAppointmentBooked(AppointmentBookedEvent event) {
        log.debug("onAppointmentBooked: appointmentId={}, patientId={}", event.appointmentId(), event.patientId());
        try {
            PatientDto patient = patientManagementFacade.getPatient(event.patientId());
            if (patient.email() == null) {
                log.debug("Patient {} has no email — skipping notification", event.patientId());
                return;
            }

            ZoneId zone = ZoneId.of("Europe/Bucharest");
            String appointmentDate = event.startAt().atZone(zone).format(DATE_FORMATTER);
            String appointmentTime = event.startAt().atZone(zone).format(TIME_FORMATTER);

            Map<String, String> variables = buildPatientVariables(patient);
            variables.put("appointmentDate", appointmentDate);
            variables.put("appointmentTime", appointmentTime);

            ruleEngine.evaluate(
                    event.tenantId(),
                    "APPOINTMENT_CONFIRMED",
                    patient.email(),
                    event.patientId(),
                    variables
            );
        } catch (Exception ex) {
            log.error("Error handling AppointmentBookedEvent for appointment {}: {}", event.appointmentId(), ex.getMessage(), ex);
        }
    }

    /**
     * Handles appointment status changes. Reacts to NO_SHOW and explicit CONFIRMED transitions.
     */
    @ApplicationModuleListener
    public void onAppointmentStatusChanged(AppointmentStatusChangedEvent event) {
        log.debug("onAppointmentStatusChanged: appointmentId={}, newStatus={}", event.appointmentId(), event.newStatus());

        String triggerType = switch (event.newStatus()) {
            case CANCELLED -> "APPOINTMENT_CANCELLED";
            case NO_SHOW   -> "APPOINTMENT_NO_SHOW";
            case CONFIRMED -> "APPOINTMENT_CONFIRMED";
            default -> null;
        };

        if (triggerType == null) return;

        try {
            PatientDto patient = patientManagementFacade.getPatient(event.patientId());
            if (patient.email() == null) {
                log.debug("Patient {} has no email — skipping notification", event.patientId());
                return;
            }

            Map<String, String> variables = buildPatientVariables(patient);

            ruleEngine.evaluate(
                    event.tenantId(),
                    triggerType,
                    patient.email(),
                    event.patientId(),
                    variables
            );
        } catch (Exception ex) {
            log.error("Error handling AppointmentStatusChangedEvent for appointment {}: {}", event.appointmentId(), ex.getMessage(), ex);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, String> buildPatientVariables(PatientDto patient) {
        Map<String, String> vars = new HashMap<>();
        String fullName = (patient.firstName() != null ? patient.firstName() : "") +
                          " " + (patient.lastName() != null ? patient.lastName() : "");
        vars.put("patientName", fullName.trim());
        vars.put("patientFirstName", patient.firstName() != null ? patient.firstName() : "");
        return vars;
    }
}
