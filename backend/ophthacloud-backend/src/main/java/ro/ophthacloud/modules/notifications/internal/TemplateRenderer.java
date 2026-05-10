package ro.ophthacloud.modules.notifications.internal;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Renders notification message templates using a simple Mustache-compatible
 * variable substitution strategy.
 * <p>
 * Supported variables (GUIDE_06 §7.3):
 * <ul>
 *   <li>{@code {{patientName}}}         — full name of the patient</li>
 *   <li>{@code {{patientFirstName}}}     — first name of the patient</li>
 *   <li>{@code {{appointmentDate}}}      — localized appointment date</li>
 *   <li>{@code {{appointmentTime}}}      — appointment time (HH:mm)</li>
 *   <li>{@code {{doctorName}}}           — doctor's full name</li>
 *   <li>{@code {{clinicName}}}           — clinic display name</li>
 *   <li>{@code {{clinicPhone}}}          — clinic contact phone</li>
 *   <li>{@code {{orderNumber}}}          — optical order number</li>
 *   <li>{@code {{prescriptionExpiry}}}   — prescription expiry date</li>
 *   <li>{@code {{portalUrl}}}            — patient portal URL</li>
 * </ul>
 * <p>
 * Missing variables are left as-is (e.g. {@code {{unknownVar}}} → {@code {{unknownVar}}}).
 * Null values in the variable map are rendered as an empty string.
 */
@Component
public class TemplateRenderer {

    /**
     * Renders a template string by replacing all {@code {{key}}} tokens with
     * values from the provided variable map.
     *
     * @param template  the template string (may be null or empty)
     * @param variables a map of variable names to replacement values
     * @return the rendered string, or an empty string if {@code template} is null/empty
     */
    public String render(String template, Map<String, String> variables) {
        if (template == null || template.isBlank()) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String token = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(token, value);
        }
        return result;
    }
}
