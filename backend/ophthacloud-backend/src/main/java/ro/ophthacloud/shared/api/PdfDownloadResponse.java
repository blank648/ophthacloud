package ro.ophthacloud.shared.api;

import java.time.Instant;

/**
 * Response DTO for PDF download endpoints.
 * Matches GUIDE_04 §6.6 response: { "downloadUrl": "...", "expiresAt": "..." }
 */
public record PdfDownloadResponse(
        String downloadUrl,
        Instant expiresAt
) {
}
