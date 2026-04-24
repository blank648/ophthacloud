package ro.ophthacloud.modules.emr.dto;

/**
 * Request body for POST /api/v1/emr/consultations/{id}/sign (GUIDE_04 §4.6).
 */
public record SignConsultationRequest(
        boolean signatureConfirmation
) {}
