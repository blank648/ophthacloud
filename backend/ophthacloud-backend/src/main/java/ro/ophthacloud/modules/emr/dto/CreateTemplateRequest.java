package ro.ophthacloud.modules.emr.dto;

/**
 * Request body for POST /api/v1/emr/templates and PUT /api/v1/emr/templates/{id} (GUIDE_04 §4.8).
 * templateData is stored as JSONB — caller supplies a raw JSON string.
 */
public record CreateTemplateRequest(
        String name,
        String sectionCode,
        String category,
        boolean isGlobal,
        String templateData
) {}
