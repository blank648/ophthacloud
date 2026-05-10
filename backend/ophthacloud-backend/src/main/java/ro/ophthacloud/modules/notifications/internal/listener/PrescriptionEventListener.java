package ro.ophthacloud.modules.notifications.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ro.ophthacloud.modules.notifications.internal.NotificationRuleEngine;
import ro.ophthacloud.modules.patients.PatientManagementFacade;
import ro.ophthacloud.modules.patients.dto.PatientDto;
import ro.ophthacloud.modules.prescriptions.event.PrescriptionSignedEvent;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrescriptionEventListener {

    private final NotificationRuleEngine  ruleEngine;
    private final PatientManagementFacade patientManagementFacade;

    /**
     * Handles prescription signing. Notifies patient with prescription reference.
     */
    @ApplicationModuleListener
    public void onPrescriptionSigned(PrescriptionSignedEvent event) {
        log.debug("onPrescriptionSigned: prescriptionId={}, patientId={}", event.prescriptionId(), event.patientId());
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

            ruleEngine.evaluate(
                    event.tenantId(),
                    "PRESCRIPTION_SIGNED",
                    patient.email(),
                    event.patientId(),
                    variables
            );
        } catch (Exception ex) {
            log.error("Error handling PrescriptionSignedEvent for prescription {}: {}", event.prescriptionId(), ex.getMessage(), ex);
        }
    }
}
