-- 9. Module: prescriptions

CREATE TABLE prescriptions (
    id                  UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID                    NOT NULL REFERENCES tenants(id),
    patient_id          UUID                    NOT NULL REFERENCES patients(id),
    consultation_id     UUID                    REFERENCES consultations(id),
    prescription_number VARCHAR(32)             NOT NULL,
    prescription_type   prescription_type       NOT NULL,
    status              prescription_status_type NOT NULL DEFAULT 'ACTIVE',
    issued_by_id        UUID                    NOT NULL,
    issued_by_name      VARCHAR(256)            NOT NULL,
    issued_at           TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    valid_from          DATE                    NOT NULL DEFAULT CURRENT_DATE,
    valid_until         DATE                    NOT NULL,
    pd_binocular        DECIMAL(5,1),
    pd_od               DECIMAL(5,1),
    pd_os               DECIMAL(5,1),
    prism_od            VARCHAR(64),
    prism_os            VARCHAR(64),
    lens_type           lens_type,
    lens_material       VARCHAR(128),
    lens_coating        VARCHAR(256),
    frame_recommendation TEXT,
    clinical_notes      TEXT,
    patient_instructions TEXT,
    superseded_by_id    UUID REFERENCES prescriptions(id),
    qr_code_token       UUID            NOT NULL DEFAULT gen_random_uuid(),
    signed_at           TIMESTAMPTZ,
    pdf_path            VARCHAR(1024),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prescriptions_tenant_patient ON prescriptions(tenant_id, patient_id);
CREATE INDEX idx_prescriptions_status         ON prescriptions(tenant_id, status);
CREATE INDEX idx_prescriptions_valid_until    ON prescriptions(tenant_id, valid_until);
CREATE INDEX idx_prescriptions_qr_token       ON prescriptions(qr_code_token);

-- 9.2 prescription_lines

CREATE TABLE prescription_lines (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    prescription_id     UUID        NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    eye                 CHAR(2)     NOT NULL,
    sph                 DECIMAL(5,2),
    cyl                 DECIMAL(5,2),
    axis                SMALLINT,
    add_power           DECIMAL(5,2),
    va_sc               VARCHAR(16),
    va_cc               VARCHAR(16),
    bcva                VARCHAR(16),
    seq                 DECIMAL(5,2),
    CONSTRAINT chk_eye CHECK (eye IN ('OD', 'OS')),
    CONSTRAINT chk_axis_range CHECK (axis IS NULL OR (axis >= 0 AND axis <= 180))
);

CREATE INDEX idx_rx_lines_prescription ON prescription_lines(tenant_id, prescription_id);
