package ro.ophthacloud.modules.emr.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after a consultation is digitally signed.
 * Consumed by downstream modules (e.g. Investigations IOP copy-over, Notifications, Prescriptions).
 * Per GUIDE_06 §3.6 and GUIDE_04 §4.6.
 */
public record ConsultationSignedEvent(
        UUID consultationId,
        UUID patientId,
        UUID tenantId,
        UUID signedById,
        String doctorName,
        Instant signedAt
) {}
