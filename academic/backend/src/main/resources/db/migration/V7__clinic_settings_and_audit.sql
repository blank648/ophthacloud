CREATE TABLE clinic_settings (
    clinic_id              INT       PRIMARY KEY,
    rewards_switch_enabled BOOLEAN   NOT NULL DEFAULT TRUE,
    tier_thresholds_json   JSONB     NOT NULL,
    updated_at             TIMESTAMP NOT NULL,

    CONSTRAINT chk_clinic_settings_singleton CHECK (clinic_id = 1)
);

INSERT INTO clinic_settings (clinic_id, rewards_switch_enabled, tier_thresholds_json, updated_at)
VALUES (1, TRUE, '{"300":5,"600":10,"1000":15}'::jsonb, NOW() AT TIME ZONE 'UTC');

CREATE TABLE gdpr_audit_logs (
    id                BIGSERIAL   PRIMARY KEY,
    actor_user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    target_patient_id UUID        REFERENCES patients(id) ON DELETE RESTRICT,
    action_type       VARCHAR(64) NOT NULL,
    ip_address        INET        NOT NULL,
    created_at        TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_gdpr_audit_logs_target_created_id ON gdpr_audit_logs (target_patient_id, created_at, id);
CREATE INDEX idx_gdpr_audit_logs_created_id ON gdpr_audit_logs (created_at, id);

CREATE OR REPLACE FUNCTION fn_prevent_gdpr_audit_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'gdpr_audit_logs is append-only: % is forbidden', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$;

CREATE TRIGGER trg_gdpr_audit_logs_immutable
    BEFORE UPDATE OR DELETE ON gdpr_audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION fn_prevent_gdpr_audit_mutation();

CREATE TRIGGER trg_gdpr_audit_logs_no_truncate
    BEFORE TRUNCATE ON gdpr_audit_logs
    FOR EACH STATEMENT
    EXECUTE FUNCTION fn_prevent_gdpr_audit_mutation();
