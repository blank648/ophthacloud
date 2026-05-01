CREATE TABLE investigations (
    id                  UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID                        NOT NULL REFERENCES tenants(id),
    patient_id          UUID                        NOT NULL REFERENCES patients(id),
    consultation_id     UUID                        REFERENCES consultations(id),
    ordered_by_id       UUID                        NOT NULL,
    ordered_by_name     VARCHAR(256)                NOT NULL,
    category            investigation_category_type NOT NULL,
    name                VARCHAR(256)                NOT NULL,
    device              VARCHAR(128),
    status              investigation_status_type   NOT NULL DEFAULT 'ORDERED',
    ordered_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    performed_at        TIMESTAMPTZ,
    result_data         JSONB,
    interpretation      TEXT,
    is_urgent           BOOLEAN                     NOT NULL DEFAULT FALSE,
    notes               TEXT,
    performed_by_id     UUID,
    created_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_investigations_tenant_patient  ON investigations(tenant_id, patient_id);
CREATE INDEX idx_investigations_consultation    ON investigations(tenant_id, consultation_id);
CREATE INDEX idx_investigations_category        ON investigations(tenant_id, category);
CREATE INDEX idx_investigations_status          ON investigations(tenant_id, status);

CREATE TABLE investigation_files (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    investigation_id    UUID        NOT NULL REFERENCES investigations(id) ON DELETE CASCADE,
    file_name           VARCHAR(512) NOT NULL,
    storage_path        VARCHAR(1024) NOT NULL,
    mime_type           VARCHAR(128),
    file_size_bytes     BIGINT,
    file_type           VARCHAR(32),
    laterality          VARCHAR(8),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inv_files_investigation ON investigation_files(tenant_id, investigation_id);
