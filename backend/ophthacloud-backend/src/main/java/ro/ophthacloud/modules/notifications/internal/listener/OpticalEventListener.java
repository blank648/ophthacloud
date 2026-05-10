package ro.ophthacloud.modules.notifications.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ro.ophthacloud.modules.notifications.internal.NotificationRuleEngine;
import ro.ophthacloud.modules.optical.event.LowStockAlertEvent;
import ro.ophthacloud.modules.optical.event.OpticalOrderReadyEvent;
import ro.ophthacloud.modules.patients.PatientManagementFacade;
import ro.ophthacloud.modules.patients.dto.PatientDto;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens to optical module Spring Modulith events and triggers notifications.
 * <p>
 * Handles:
 * <ul>
 *   <li>{@link OpticalOrderReadyEvent} → trigger {@code ORDER_READY} (patient notification)</li>
 *   <li>{@link LowStockAlertEvent}     → trigger {@code LOW_STOCK} (admin alert)</li>
 * </ul>
 * Per GUIDE_06 §7.2.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpticalEventListener {

    private final NotificationRuleEngine  ruleEngine;
    private final PatientManagementFacade patientManagementFacade;

    /**
     * Handles optical order ready-for-fitting event.
     * Notifies the patient that their glasses/lenses are ready for collection.
     */
    @ApplicationModuleListener
    public void onOpticalOrderReady(OpticalOrderReadyEvent event) {
        log.debug("onOpticalOrderReady: orderId={}, patientId={}", event.orderId(), event.patientId());
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
            variables.put("orderNumber", event.orderNumber() != null ? event.orderNumber() : "");

            ruleEngine.evaluate(
                    event.tenantId(),
                    "ORDER_READY",
                    patient.email(),
                    event.patientId(),
                    variables
            );
        } catch (Exception ex) {
            log.error("Error handling OpticalOrderReadyEvent for order {}: {}", event.orderId(), ex.getMessage(), ex);
        }
    }

    /**
     * Handles low-stock alert events.
     * Sends an admin alert email — no patient ID in this case.
     * The recipient address for LOW_STOCK is looked up from the tenant's active notification rules.
     */
    @ApplicationModuleListener
    public void onLowStockAlert(LowStockAlertEvent event) {
        log.debug("onLowStockAlert: stockItemId={}, itemName={}", event.stockItemId(), event.itemName());
        try {
            Map<String, String> variables = new HashMap<>();
            variables.put("itemName", event.itemName() != null ? event.itemName() : "");
            variables.put("sku",          event.sku() != null ? event.sku() : "");
            variables.put("currentStock", String.valueOf(event.currentStock()));
            variables.put("minimumStock", String.valueOf(event.minimumStock()));

            // LOW_STOCK notifications are admin-facing; recipient is determined per rule configuration.
            // We use a sentinel value here — the rule's configured recipient will override in a full
            // implementation. For now we pass a placeholder that rules are expected to contain the
            // admin address in their template config_data (see GUIDE_06 §7.2).
            ruleEngine.evaluate(
                    event.tenantId(),
                    "LOW_STOCK",
                    "admin",    // recipient resolved from rule config in full implementation
                    null,       // no patient for stock alerts
                    variables
            );
        } catch (Exception ex) {
            log.error("Error handling LowStockAlertEvent for stockItem {}: {}", event.stockItemId(), ex.getMessage(), ex);
        }
    }
}
