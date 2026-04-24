ALTER TABLE appointments ADD COLUMN IF NOT EXISTS consultation_id UUID;

CREATE TABLE consultations (
    id                  UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID                        NOT NULL REFERENCES tenants(id),
    patient_id          UUID                        NOT NULL REFERENCES patients(id),
    appointment_id      UUID                        REFERENCES appointments(id),
    doctor_id           UUID                        NOT NULL,
    doctor_name         VARCHAR(256)                NOT NULL,
    doctor_signature    VARCHAR(512),
    status              consultation_status_type    NOT NULL DEFAULT 'DRAFT',
    consultation_date   DATE                        NOT NULL DEFAULT CURRENT_DATE,
    chief_complaint     TEXT,
    sections_completed  SMALLINT                    DEFAULT 0,
    signed_at           TIMESTAMPTZ,
    signed_by_id        UUID,
    created_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    version             BIGINT                      NOT NULL DEFAULT 0,
    CONSTRAINT uq_consultation_appointment UNIQUE (appointment_id)
);

CREATE INDEX idx_consultations_tenant_patient  ON consultations(tenant_id, patient_id);
CREATE INDEX idx_consultations_date            ON consultations(tenant_id, consultation_date DESC);
CREATE INDEX idx_consultations_doctor          ON consultations(tenant_id, doctor_id);
CREATE INDEX idx_consultations_status          ON consultations(tenant_id, status);

-- Update appointments FK
ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_consultation
    FOREIGN KEY (consultation_id) REFERENCES consultations(id);

CREATE TABLE consultation_sections (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    consultation_id UUID        NOT NULL REFERENCES consultations(id) ON DELETE CASCADE,
    section_code    VARCHAR(1)  NOT NULL,
    section_data    JSONB       NOT NULL DEFAULT '{}',
    is_completed    BOOLEAN     NOT NULL DEFAULT FALSE,
    completed_at    TIMESTAMPTZ,
    template_id     UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_section_per_consultation UNIQUE (consultation_id, section_code),
    CONSTRAINT chk_section_code CHECK (section_code IN ('A','B','C','D','E','F','G','H','I'))
);

CREATE INDEX idx_sections_consultation ON consultation_sections(tenant_id, consultation_id);
CREATE INDEX idx_sections_data_gin     ON consultation_sections USING gin(section_data);

CREATE TABLE clinical_templates (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    name            VARCHAR(256) NOT NULL,
    section_code    VARCHAR(1)  NOT NULL,
    template_data   JSONB       NOT NULL,
    category        VARCHAR(128),
    is_global       BOOLEAN     DEFAULT FALSE,
    created_by_id   UUID,
    is_active       BOOLEAN     DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_templates_tenant_section ON clinical_templates(tenant_id, section_code);
