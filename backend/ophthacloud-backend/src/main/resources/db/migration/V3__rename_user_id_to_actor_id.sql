ALTER TABLE audit_log RENAME COLUMN user_id TO actor_id;
DROP INDEX IF EXISTS idx_audit_log_user_id;
CREATE INDEX idx_audit_log_actor_id ON audit_log(actor_id);
