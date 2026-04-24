package ro.ophthacloud.modules.emr.internal;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class SectionDataValidator {

    private final ObjectMapper objectMapper;

    public SectionDataValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validateSectionData(String sectionCode, String sectionDataJson) {
        if (!"A".equals(sectionCode)) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(sectionDataJson);
            validateEyeNode(root.path("od"));
            validateEyeNode(root.path("os"));

            Double pdBin = getDoubleOrNull(root.path("pd_binocular"));
            Double pdOd = getDoubleOrNull(root.path("od").path("pd"));
            Double pdOs = getDoubleOrNull(root.path("os").path("pd"));

            if (pdBin != null) {
                if (pdBin < 50.0 || pdBin > 80.0) {
                    throw new IllegalArgumentException("PD binocular must be between 50.0 and 80.0");
                }
                if (pdOd != null && pdOs != null) {
                    double sum = pdOd + pdOs;
                    if (Math.abs(sum - pdBin) > 2.001) {
                        throw new IllegalArgumentException("Sum of monocular PDs must be equal to binocular PD ±2mm");
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format for section data", e);
        }
    }

    private void validateEyeNode(JsonNode eyeNode) {
        if (eyeNode.isMissingNode() || eyeNode.isNull()) {
            return;
        }

        Double sph = getDoubleOrNull(eyeNode.path("sph"));
        if (sph != null && (sph < -30.0 || sph > 30.0)) {
            throw new IllegalArgumentException("Sph must be between -30.00 and +30.00");
        }

        Double cyl = getDoubleOrNull(eyeNode.path("cyl"));
        if (cyl != null && (cyl < -10.0 || cyl > 10.0)) {
            throw new IllegalArgumentException("Cyl must be between -10.00 and +10.00");
        }

        Integer axis = getIntegerOrNull(eyeNode.path("axis"));
        if (axis != null && (axis < 1 || axis > 180)) {
            throw new IllegalArgumentException("Axis must be between 1 and 180");
        }

        Double add = getDoubleOrNull(eyeNode.path("add"));
        if (add != null && (add < 0.50 || add > 4.00)) {
            throw new IllegalArgumentException("Add must be between 0.50 and 4.00");
        }
    }

    private Double getDoubleOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asDouble();
    }

    private Integer getIntegerOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asInt();
    }
}
