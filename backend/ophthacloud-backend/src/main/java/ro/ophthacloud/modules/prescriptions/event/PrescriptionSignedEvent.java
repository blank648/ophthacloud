package ro.ophthacloud.modules.prescriptions.event;

import ro.ophthacloud.modules.prescriptions.enums.PrescriptionType;

import java.time.Instant;
import java.util.UUID;

public record PrescriptionSignedEvent(
    UUID prescriptionId,
    UUID patientId,
    UUID tenantId,
    PrescriptionType prescriptionType,
    String prescriptionNumber,
    Instant signedAt
) {
}
