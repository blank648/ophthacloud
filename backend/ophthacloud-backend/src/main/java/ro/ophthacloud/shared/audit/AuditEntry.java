package ro.ophthacloud.shared.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable value object (builder pattern) representing a single audit event.
 * <p>
 * Created by callers in service methods and passed to {@link AuditLogService#log(AuditEntry)}.
 * The service fills in {@code tenantId}, {@code userId}, and {@code ipAddress} automatically
 * from security and request context — callers must NOT set those fields.
 * <p>
 * Usage example:
 * <pre>{@code
 * auditLogService.log(
 *     AuditEntry.builder()
 *         .action("CREATE")
 *         .entityType("Patient")
 *         .entityId(patient.getId())
 *         .changedField("mrn",        null,               patient.getMrn())
 *         .changedField("firstName",  null,               patient.getFirstName())
 *         .build()
 * );
 * }</pre>
 *
 * Per GUIDE_05 §7.1 and GUIDE_07 §1.3.
 */
public final class AuditEntry {

    /** Action string. Canonical values: CREATE | UPDATE | DELETE | SIGN | EXPORT | VIEW. */
    private final String action;

    /** Simple class name of the audited entity, e.g. {@code "Patient"}. */
    private final String entityType;

    /** Primary key of the audited entity. Null for bulk or list operations. */
    private final UUID entityId;

    /**
     * Field-level change diff.
     * Keys are field names; values are {@code {"old": ..., "new": ...}} maps.
     * May be empty or null for DELETE / VIEW actions.
     */
    private final Map<String, Object> changedFields;

    private AuditEntry(Builder builder) {
        this.action        = builder.action;
        this.entityType    = builder.entityType;
        this.entityId      = builder.entityId;
        this.changedFields = builder.changedFields.isEmpty() ? null : Map.copyOf(builder.changedFields);
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    /** @return audit action string (CREATE, UPDATE, DELETE, SIGN, EXPORT, VIEW) */
    public String getAction() { return action; }

    /** @return simple class name of the audited entity */
    public String getEntityType() { return entityType; }

    /** @return entity primary key, or null for bulk operations */
    public UUID getEntityId() { return entityId; }

    /**
     * @return field-level change map, or null if no fields were recorded.
     *         Structure: {@code {"fieldName": {"old": "prev", "new": "next"}}}
     */
    public Map<String, Object> getChangedFields() { return changedFields; }

    // ── Factory ──────────────────────────────────────────────────────────────

    /** Returns a new {@link Builder}. */
    public static Builder builder() {
        return new Builder();
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    /**
     * Fluent builder for {@link AuditEntry}.
     * <p>
     * The {@code changedField(name, oldValue, newValue)} helper stores both old and new
     * values in the diff map so the audit trail is self-contained and human-readable.
     */
    public static final class Builder {

        private String              action;
        private String              entityType;
        private UUID                entityId;
        private final Map<String, Object> changedFields = new HashMap<>();

        private Builder() {}

        /**
         * Sets the action verb.
         *
         * @param action CREATE | UPDATE | DELETE | SIGN | EXPORT | VIEW
         * @return this builder
         */
        public Builder action(String action) {
            this.action = action;
            return this;
        }

        /**
         * Sets the simple class name of the audited entity.
         *
         * @param entityType e.g. {@code "Patient"}, {@code "Consultation"}
         * @return this builder
         */
        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        /**
         * Sets the primary key of the audited entity.
         *
         * @param entityId entity UUID; may be null for bulk operations
         * @return this builder
         */
        public Builder entityId(UUID entityId) {
            this.entityId = entityId;
            return this;
        }

        /**
         * Records a single field change.  Both old and new values may be null.
         * The entry is stored as {@code {"old": oldValue, "new": newValue}}.
         *
         * @param fieldName field name (Java camelCase or DB column name)
         * @param oldValue  previous value — null means "field was not set before"
         * @param newValue  new value — null means "field was cleared"
         * @return this builder
         */
        public Builder changedField(String fieldName, Object oldValue, Object newValue) {
            Map<String, Object> diff = new HashMap<>(2);
            diff.put("old", oldValue);
            diff.put("new", newValue);
            this.changedFields.put(fieldName, diff);
            return this;
        }

        /**
         * Builds and validates the {@link AuditEntry}.
         *
         * @return immutable AuditEntry
         * @throws IllegalStateException if {@code action} or {@code entityType} is missing
         */
        public AuditEntry build() {
            if (action == null || action.isBlank()) {
                throw new IllegalStateException("AuditEntry.action must not be blank");
            }
            if (entityType == null || entityType.isBlank()) {
                throw new IllegalStateException("AuditEntry.entityType must not be blank");
            }
            return new AuditEntry(this);
        }
    }
}
