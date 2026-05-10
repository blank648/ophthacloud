package ro.ophthacloud.modules.notifications.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ro.ophthacloud.modules.emr.event.ConsultationSignedEvent;
import ro.ophthacloud.modules.notifications.internal.NotificationRuleEngine;
import ro.ophthacloud.modules.patients.PatientManagementFacade;
import ro.ophthacloud.modules.patients.dto.PatientDto;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens to EMR consultation-related Spring Modulith events and triggers notifications.
 * <p>
 * Handles:
 * <ul>
 *   <li>{@link ConsultationSignedEvent} → trigger {@code CONSULTATION_SIGNED}</li>
 * </ul>
 * Per GUIDE_06 §7.2.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmrEventListener {

    private final NotificationRuleEngine  ruleEngine;
    private final PatientManagementFacade patientManagementFacade;

    /**
     * Handles consultation signing. Notifies patient that their clinical record is finalized.
     */
    @ApplicationModuleListener
    public void onConsultationSigned(ConsultationSignedEvent event) {
        log.debug("onConsultationSigned: consultationId={}, patientId={}", event.consultationId(), event.patientId());
        try {
            PatientDto patient = patientManagementFacade.getPatient(event.patientId());
            if (patient.email() == null) {
                log.debug("Patient {} has no email — skipping notification", event.patientId());
                return;
            }

            Map<String, String> variables = new HashMap<>();
            String fullName = (patient.firstName() != null ? patient.firstName() : "") +
                              " " + (patient.lastName() != null ? patient.lastName() : "");
            variables.put("patientName", fullName.trim());
            variables.put("patientFirstName", patient.firstName() != null ? patient.firstName() : "");
            variables.put("doctorName", event.doctorName() != null ? event.doctorName() : "");

            ruleEngine.evaluate(
                    event.tenantId(),
                    "CONSULTATION_SIGNED",
                    patient.email(),
                    event.patientId(),
                    variables
            );
        } catch (Exception ex) {
            log.error("Error handling ConsultationSignedEvent for consultation {}: {}", event.consultationId(), ex.getMessage(), ex);
        }
    }
}
