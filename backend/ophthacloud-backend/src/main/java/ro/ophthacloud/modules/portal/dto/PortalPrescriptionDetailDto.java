package ro.ophthacloud.modules.portal.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Full prescription detail for portal patients, including lines and QR token.
 */
public record PortalPrescriptionDetailDto(
        UUID id,
        String type,
        String status,
        String issuedByName,
        LocalDate issuedDate,
        LocalDate validUntil,
        List<PortalPrescriptionLineDto> lines,
        UUID qrVerificationToken
) {}
