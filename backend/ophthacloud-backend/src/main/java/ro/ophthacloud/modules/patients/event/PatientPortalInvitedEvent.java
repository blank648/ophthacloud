package ro.ophthacloud.modules.patients.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after a patient is successfully invited to the Patient Portal.
 * The notifications module listens for this event to trigger the invite email.
 */
public record PatientPortalInvitedEvent(
        UUID patientId,
        String email,
        UUID tenantId,
        Instant invitedAt
) {}
