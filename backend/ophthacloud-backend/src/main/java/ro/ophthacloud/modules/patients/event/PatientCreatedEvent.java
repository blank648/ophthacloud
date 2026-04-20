package ro.ophthacloud.modules.patients.event;

import java.util.UUID;

/**
 * Published by {@link ro.ophthacloud.modules.patients.PatientManagementFacade}
 * after a new patient is successfully persisted.
 * <p>
 * Listeners (e.g. notifications module) can react to this event via
 * {@code @TransactionalEventListener} without creating a compile-time dependency
 * on the patients module internal classes.
 */
public record PatientCreatedEvent(
        UUID patientId,
        String mrn,
        UUID tenantId
) {}
