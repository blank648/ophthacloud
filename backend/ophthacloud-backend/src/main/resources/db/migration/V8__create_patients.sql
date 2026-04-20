-- V8__create_patients.sql
-- Create patients module tables: patients, patient_medical_history, patient_consents, patient_attachments

CREATE TABLE patients (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID            NOT NULL REFERENCES tenants(id),
    mrn                 VARCHAR(32)     NOT NULL,    -- Medical Record Number (OC-XXXXXX)
    first_name          VARCHAR(128)    NOT NULL,
    last_name           VARCHAR(128)    NOT NULL,
    date_of_birth       DATE            NOT NULL,
    gender              gender_type     NOT NULL,
    cnp                 VARCHAR(13),                 -- Romanian Personal ID (optional, sensitive)
    phone               VARCHAR(32),
    phone_alt           VARCHAR(32),
    email               VARCHAR(256),
    address             TEXT,
    city                VARCHAR(128),
    county              VARCHAR(64),
    blood_type          VARCHAR(8),                  -- 'A+', 'B-', 'O+', 'AB+', etc.
    occupation          VARCHAR(128),
    employer            VARCHAR(256),
    emergency_contact_name  VARCHAR(256),
    emergency_contact_phone VARCHAR(32),
    insurance_provider  VARCHAR(256),
    insurance_number    VARCHAR(128),
    referring_doctor    VARCHAR(256),
    has_portal_access   BOOLEAN         NOT NULL DEFAULT FALSE,
    portal_invited_at   TIMESTAMPTZ,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    notes               TEXT,
    avatar_url          VARCHAR(1024),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version             BIGINT,
    CONSTRAINT uq_patients_tenant_mrn UNIQUE (tenant_id, mrn)
);

CREATE INDEX idx_patients_tenant_id       ON patients(tenant_id);
CREATE INDEX idx_patients_last_name       ON patients(tenant_id, last_name);
CREATE INDEX idx_patients_dob             ON patients(tenant_id, date_of_birth);
CREATE INDEX idx_patients_phone           ON patients(tenant_id, phone);
CREATE INDEX idx_patients_email           ON patients(tenant_id, email);
CREATE INDEX idx_patients_full_name       ON patients USING gin(to_tsvector('simple', first_name || ' ' || last_name));

CREATE TABLE patient_medical_history (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    patient_id          UUID        NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    -- Systemic history (boolean flags)
    has_diabetes        BOOLEAN     DEFAULT FALSE,
    has_hypertension    BOOLEAN     DEFAULT FALSE,
    has_cardiovascular  BOOLEAN     DEFAULT FALSE,
    has_thyroid         BOOLEAN     DEFAULT FALSE,
    has_autoimmune      BOOLEAN     DEFAULT FALSE,
    -- Ophthalmic history (boolean flags)
    has_glaucoma_history    BOOLEAN DEFAULT FALSE,
    has_cataract_history    BOOLEAN DEFAULT FALSE,
    has_amd_history         BOOLEAN DEFAULT FALSE,
    has_retinal_detachment  BOOLEAN DEFAULT FALSE,
    has_strabismus          BOOLEAN DEFAULT FALSE,
    -- Surgical history
    has_previous_eye_surgery BOOLEAN DEFAULT FALSE,
    eye_surgeries_detail    TEXT,
    -- Allergies and medications
    known_allergies         TEXT,
    current_medications     TEXT,
    -- Family history
    family_glaucoma         BOOLEAN DEFAULT FALSE,
    family_amd              BOOLEAN DEFAULT FALSE,
    family_other            TEXT,
    -- Active diagnosis list (maintained by EMR module from Section G data)
    active_diagnoses        JSONB   DEFAULT '[]',   -- array of {icd10_code, icd10_name, laterality, since_date}
    -- Free text
    additional_notes        TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 BIGINT,
    CONSTRAINT uq_pmh_patient UNIQUE (patient_id)
);

CREATE INDEX idx_pmh_tenant_patient ON patient_medical_history(tenant_id, patient_id);

CREATE TABLE patient_consents (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    patient_id      UUID        NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    consent_type    VARCHAR(64) NOT NULL,   -- 'GENERAL_TREATMENT', 'DATA_PROCESSING_GDPR', 'PORTAL_ACCESS', 'PHOTOGRAPHY', 'RESEARCH'
    is_granted      BOOLEAN     NOT NULL,
    granted_at      TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ,
    signed_by_name  VARCHAR(256),           -- patient name as signed
    document_url    VARCHAR(1024),          -- PDF of signed consent form
    ip_address      VARCHAR(64),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT
);

CREATE INDEX idx_consents_patient ON patient_consents(tenant_id, patient_id);
CREATE INDEX idx_consents_type    ON patient_consents(tenant_id, consent_type);

CREATE TABLE patient_attachments (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    patient_id      UUID        NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    file_name       VARCHAR(512) NOT NULL,
    file_size_bytes BIGINT,
    mime_type       VARCHAR(128),
    storage_path    VARCHAR(1024) NOT NULL,  -- MinIO path
    category        VARCHAR(64),             -- 'REFERRAL', 'PREVIOUS_PRESCRIPTION', 'ID_DOCUMENT', 'OTHER'
    uploaded_by_id  UUID,                    -- staff member UUID
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT
);

CREATE INDEX idx_attachments_patient ON patient_attachments(tenant_id, patient_id);
