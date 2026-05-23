package ro.ophthacloud.modules.portal.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for updating patient GDPR consents via the portal.
 */
public record UpdateConsentsRequest(
        @NotNull Boolean dataProcessingConsent,
        @NotNull Boolean communicationConsent,
        Boolean researchConsent
) {}
