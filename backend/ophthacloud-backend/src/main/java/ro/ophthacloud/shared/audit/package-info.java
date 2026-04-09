/**
 * Shared audit infrastructure for OphthaCloud.
 * <p>
 * Public API surface (injectable by all modules):
 * <ul>
 *   <li>{@link ro.ophthacloud.shared.audit.AuditLogService} — the only write path for audit records</li>
 *   <li>{@link ro.ophthacloud.shared.audit.AuditEntry} — immutable value object / builder for audit events</li>
 * </ul>
 * <p>
 * Internal (package-private — do NOT inject directly from other packages):
 * <ul>
 *   <li>{@link ro.ophthacloud.shared.audit.AuditLogEntity} — JPA entity</li>
 *   <li>{@link ro.ophthacloud.shared.audit.AuditLogRepository} — Spring Data repository</li>
 * </ul>
 * <p>
 * All audit records are immutable once written. The PostgreSQL trigger
 * {@code trg_audit_log_immutable} prevents UPDATE and DELETE at the DB level.
 */
package ro.ophthacloud.shared.audit;
