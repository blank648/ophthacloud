-- ============================================================
-- V2 — Audit Log Table + Immutability Trigger
-- Per GUIDE_05 §7.1 and GUIDE_07 §1.3
-- ============================================================

CREATE TABLE audit_log (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id         VARCHAR(256) NOT NULL,                   -- Keycloak user UUID (string)
    action          VARCHAR(64)  NOT NULL,                   -- CREATE | UPDATE | DELETE | SIGN | EXPORT | VIEW
    entity_type     VARCHAR(128) NOT NULL,                   -- e.g. 'Patient', 'Consultation'
    entity_id       UUID,                                    -- nullable for bulk/list operations
    changed_fields  JSONB,                                   -- field-level diff payload, nullable for DELETE/VIEW
    ip_address      VARCHAR(64),                             -- client IP (X-Forwarded-For or remoteAddr)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for per-tenant audit queries (primary access pattern)
CREATE INDEX idx_audit_log_tenant_id       ON audit_log(tenant_id);
-- Index for per-entity audit trail
CREATE INDEX idx_audit_log_entity          ON audit_log(entity_type, entity_id);
-- Index for per-user activity queries
CREATE INDEX idx_audit_log_user_id         ON audit_log(user_id);
-- Index for time-range reporting
CREATE INDEX idx_audit_log_created_at      ON audit_log(created_at DESC);
-- Composite: tenant + time (most frequent report query)
CREATE INDEX idx_audit_log_tenant_created  ON audit_log(tenant_id, created_at DESC);


-- ============================================================
-- Immutability Trigger: prevent UPDATE or DELETE on audit_log
-- Once written, an audit record is permanent.
-- Per GUIDE_05 §7.1
-- ============================================================

CREATE OR REPLACE FUNCTION enforce_audit_log_immutability()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'audit_log records are immutable. Attempted % on row id=%. Audit data may not be modified or deleted.',
        TG_OP,
        OLD.id;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_immutable
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION enforce_audit_log_immutability();
