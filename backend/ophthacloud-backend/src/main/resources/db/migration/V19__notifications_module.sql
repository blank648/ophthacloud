-- 11. Module: notifications

-- 11.1 recall_protocols
CREATE TABLE recall_protocols (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    name                VARCHAR(256) NOT NULL,
    icd10_code          VARCHAR(16),             -- trigger diagnosis
    recall_interval_months INT      NOT NULL,    -- months between recalls
    description         TEXT,
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT      DEFAULT 0
);

-- 11.2 notification_rules
CREATE TABLE notification_rules (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    name            VARCHAR(256) NOT NULL,
    config_data     JSONB       NOT NULL,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    last_run_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT      DEFAULT 0
);

CREATE INDEX idx_notif_rules_tenant ON notification_rules(tenant_id, is_active);

-- 11.3 notification_log
CREATE TABLE notification_log (
    id                  UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID                        NOT NULL REFERENCES tenants(id),
    patient_id          UUID                        REFERENCES patients(id),
    rule_id             UUID                        REFERENCES notification_rules(id),
    channel             VARCHAR(32)                 NOT NULL,
    status              VARCHAR(32)                 NOT NULL DEFAULT 'PENDING',
    recipient_address   VARCHAR(512)                NOT NULL,   -- email or phone
    subject             VARCHAR(512),
    body_preview        VARCHAR(512),               -- first 512 chars for audit display
    sent_at             TIMESTAMPTZ,
    failed_at           TIMESTAMPTZ,
    failure_reason      TEXT,
    external_message_id VARCHAR(256),               -- ID from SMS/email provider
    created_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    version             BIGINT                      DEFAULT 0
);

CREATE INDEX idx_notif_log_tenant_patient ON notification_log(tenant_id, patient_id);
CREATE INDEX idx_notif_log_status         ON notification_log(tenant_id, status);
CREATE INDEX idx_notif_log_date           ON notification_log(tenant_id, created_at DESC);
