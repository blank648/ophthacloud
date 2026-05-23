package ro.ophthacloud.modules.portal.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Prescription summary for portal listing.
 */
public record PortalPrescriptionSummaryDto(
        UUID id,
        String type,
        String status,
        String issuedByName,
        LocalDate issuedDate,
        LocalDate validUntil
) {}
