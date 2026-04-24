package ro.ophthacloud.modules.emr.dto;

import java.time.LocalDate;
import java.util.UUID;

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
