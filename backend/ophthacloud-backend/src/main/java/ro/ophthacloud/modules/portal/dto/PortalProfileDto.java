package ro.ophthacloud.modules.portal.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Patient's own profile as seen in the portal.
 * Excludes sensitive internal fields (CNP, notes, insurance details).
 */
public record PortalProfileDto(
        UUID id,
        String mrn,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String phone,
        String email,
        boolean hasPortalAccess
) {}
