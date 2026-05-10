package ro.ophthacloud.modules.notifications.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererTest {

    private TemplateRenderer templateRenderer;

    @BeforeEach
    void setUp() {
        templateRenderer = new TemplateRenderer();
    }

    @Test
    @DisplayName("Should substitute variables correctly")
    void shouldSubstituteVariables() {
        String template = "Hello {{patientName}}, your appointment is at {{appointmentTime}}.";
        Map<String, String> vars = Map.of(
                "patientName", "John Doe",
                "appointmentTime", "10:30"
        );

        String result = templateRenderer.render(template, vars);

        assertThat(result).isEqualTo("Hello John Doe, your appointment is at 10:30.");
    }

    @Test
    @DisplayName("Should leave missing variables untouched")
    void shouldLeaveMissingVariablesUntouched() {
        String template = "Hello {{patientName}}, see you at {{unknownVar}}.";
        Map<String, String> vars = Map.of("patientName", "Jane Doe");

        String result = templateRenderer.render(template, vars);

        assertThat(result).isEqualTo("Hello Jane Doe, see you at {{unknownVar}}.");
    }

    @Test
    @DisplayName("Should render null map values as empty string")
    void shouldRenderNullMapValuesAsEmpty() {
        String template = "Name: {{patientName}}, Phone: {{phone}}";
        Map<String, String> vars = new HashMap<>();
        vars.put("patientName", "Alice");
        vars.put("phone", null); // explicit null

        String result = templateRenderer.render(template, vars);

        assertThat(result).isEqualTo("Name: Alice, Phone: ");
    }

    @Test
    @DisplayName("Should return empty string if template is null or blank")
    void shouldReturnEmptyForNullOrBlankTemplate() {
        Map<String, String> vars = Map.of("var", "val");

        assertThat(templateRenderer.render(null, vars)).isEmpty();
        assertThat(templateRenderer.render("", vars)).isEmpty();
        assertThat(templateRenderer.render("   ", vars)).isEmpty();
    }

    @Test
    @DisplayName("Should return original template if vars are null or empty")
    void shouldReturnOriginalTemplateIfVarsNullOrEmpty() {
        String template = "Hello {{name}}";

        assertThat(templateRenderer.render(template, null)).isEqualTo(template);
        assertThat(templateRenderer.render(template, Map.of())).isEqualTo(template);
    }
}
