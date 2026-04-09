# GUIDE_03 — Data Model: OphthaCloud

> **Document type:** Database Schema Reference  
> **Version:** 1.0  
> **Last updated:** 2026-04-02  
> **Author:** Project Architect  
> **Status:** FINAL — schema changes require a new Flyway migration file. Never edit an existing migration.  
> **Prerequisites:** GUIDE_00, GUIDE_01, GUIDE_02

---

## Purpose

This document defines the complete PostgreSQL database schema for Phase 1.
Every table, column, constraint, index, and foreign key must match exactly what is implemented.

**Rules Antigravity must follow:**
- Never alter a Flyway migration file that has already been applied — create a new one (`V{n+1}__...sql`)
- Never use `ddl-auto: create` or `ddl-auto: update` in any environment — only `validate`
- Every tenant-scoped entity must have `tenant_id UUID NOT NULL` and extend `TenantAwareEntity`
- Every table must have `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- All timestamps are `TIMESTAMPTZ` (timezone-aware), stored in UTC
- JSONB columns may only be used for variable-structure clinical data — see Section 4

---

## 1. Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Tables | `snake_case`, plural | `patients`, `optical_orders` |
| Columns | `snake_case` | `first_name`, `tenant_id`, `created_at` |
| Primary keys | `id` | `id UUID` |
| Foreign keys | `{referenced_table_singular}_id` | `patient_id`, `consultation_id` |
| Boolean columns | `is_{adjective}` or `has_{noun}` | `is_active`, `has_portal_access` |
| Timestamps | `{event}_at` | `created_at`, `signed_at`, `checked_in_at` |
| JSONB data columns | `{content}_data` | `section_data`, `result_data`, `config_data` |
| Indexes | `idx_{table}_{column(s)}` | `idx_patients_tenant_id`, `idx_appointments_date` |
| Unique constraints | `uq_{table}_{column(s)}` | `uq_patients_tenant_mrn` |
| Check constraints | `chk_{table}_{column}` | `chk_iop_value_range` |
| Enum types | `{domain}_{concept}_type` | `appointment_status_type`, `order_stage_type` |

---

## 2. System-Wide Enums

These PostgreSQL enum types are created in `V1__baseline_schema.sql`:

```sql
-- Appointment
CREATE TYPE appointment_status_type AS ENUM (
    'BOOKED', 'CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED', 'NO_SHOW', 'CANCELLED'
);
CREATE TYPE appointment_channel_type AS ENUM (
    'IN_PERSON', 'TELEMEDICINE'
);

-- Consultation
CREATE TYPE consultation_status_type AS ENUM (
    'DRAFT', 'IN_PROGRESS', 'SIGNED'
);

-- Prescription
CREATE TYPE prescription_status_type AS ENUM (
    'ACTIVE', 'EXPIRED', 'CANCELLED', 'SUPERSEDED'
);
CREATE TYPE prescription_type AS ENUM (
    'DISTANCE', 'NEAR', 'PROGRESSIVE', 'CONTACT_LENS', 'POST_OP', 'TINTED'
);
CREATE TYPE lens_type AS ENUM (
    'SINGLE_VISION', 'BIFOCAL', 'PROGRESSIVE', 'OFFICE', 'CONTACT'
);

-- Optical Order
CREATE TYPE order_stage_type AS ENUM (
    'RECEIVED', 'SENT_TO_LAB', 'QC_CHECK', 'READY_FOR_FITTING', 'COMPLETED', 'CANCELLED'
);
CREATE TYPE order_type AS ENUM (
    'GLASSES', 'CONTACT_LENSES', 'ACCESSORIES', 'SERVICE'
);

-- Investigation
CREATE TYPE investigation_status_type AS ENUM (
    'ORDERED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
);
CREATE TYPE investigation_category_type AS ENUM (
    'OCT', 'VISUAL_FIELD', 'TOPOGRAPHY', 'FUNDUS_PHOTO', 'BIOMETRY',
    'SPECULAR_MICROSCOPY', 'ELECTRORETINOGRAPHY', 'BLOOD_TEST', 'OTHER'
);

-- Invoice
CREATE TYPE invoice_status_type AS ENUM (
    'DRAFT', 'ISSUED', 'PAID', 'PARTIAL', 'CANCELLED'
);
CREATE TYPE payment_method_type AS ENUM (
    'CASH', 'CARD', 'BANK_TRANSFER', 'INSURANCE', 'VOUCHER'
);

-- Notification
CREATE TYPE notification_channel_type AS ENUM (
    'EMAIL', 'SMS', 'PUSH', 'IN_APP'
);
CREATE TYPE notification_status_type AS ENUM (
    'PENDING', 'SENT', 'FAILED', 'CANCELLED'
);

-- Staff / Access
CREATE TYPE staff_role_type AS ENUM (
    'SUPER_ADMIN', 'CLINIC_ADMIN', 'DOCTOR', 'OPTOMETRIST',
    'NURSE', 'RECEPTIONIST', 'OPTICAL_TECHNICIAN', 'MANAGER', 'PATIENT'
);

-- Gender
CREATE TYPE gender_type AS ENUM (
    'MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'
);
```

---

## 3. System Tables (No Tenant Scope)

These tables do not have `tenant_id` and do not apply the Hibernate tenant filter.

### 3.1 tenants

```sql
CREATE TABLE tenants (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(64) UNIQUE NOT NULL,          -- URL-safe: "clinica-exemplu"
    name            VARCHAR(256) NOT NULL,                 -- Display: "Clinica Exemplu SRL"
    legal_name      VARCHAR(512),                         -- for invoices
    cui             VARCHAR(20),                          -- Romanian tax ID
    address         TEXT,
    city            VARCHAR(128),
    county          VARCHAR(64),
    country         VARCHAR(64) DEFAULT 'RO',
    phone           VARCHAR(32),
    email           VARCHAR(256),
    website         VARCHAR(512),
    logo_url        VARCHAR(1024),
    keycloak_realm  VARCHAR(128) NOT NULL,                -- Keycloak realm name
    keycloak_org_id VARCHAR(256),                        -- Keycloak Organizations ID
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    subscription_plan VARCHAR(32) DEFAULT 'TRIAL',       -- TRIAL / STARTER / PRO / ENTERPRISE
    trial_ends_at   TIMESTAMPTZ,
    timezone        VARCHAR(64) DEFAULT 'Europe/Bucharest',
    locale          VARCHAR(16) DEFAULT 'ro-RO',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenants_slug ON tenants(slug);
CREATE INDEX idx_tenants_keycloak_realm ON tenants(keycloak_realm);
```

### 3.2 modules_catalog

```sql
CREATE TABLE modules_catalog (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(64) UNIQUE NOT NULL,   -- 'patients', 'emr', 'optical', ...
    name        VARCHAR(128) NOT NULL,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO modules_catalog (code, name) VALUES
    ('dashboard',      'Dashboard'),
    ('patients',       'Gestionare Pacienți'),
    ('appointments',   'Programări'),
    ('emr',            'Consultație Clinică'),
    ('investigations', 'Investigații'),
    ('prescriptions',  'Rețete / Prescripții'),
    ('optical',        'ERP Optic'),
    ('notifications',  'Notificări & Recall'),
    ('portal',         'Portal Pacienți'),
    ('reports',        'Rapoarte & KPIs'),
    ('admin',          'Setări & Administrare');
```

### 3.3 tenant_role_module_permissions

```sql
CREATE TABLE tenant_role_module_permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role        staff_role_type NOT NULL,
    module_code VARCHAR(64) NOT NULL REFERENCES modules_catalog(code),
    can_view    BOOLEAN NOT NULL DEFAULT FALSE,
    can_create  BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit    BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete  BOOLEAN NOT NULL DEFAULT FALSE,
    can_sign    BOOLEAN NOT NULL DEFAULT FALSE,    -- clinical signature
    can_export  BOOLEAN NOT NULL DEFAULT FALSE,    -- export/print
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tenant_role_module UNIQUE (tenant_id, role, module_code)
);

CREATE INDEX idx_trmp_tenant_role ON tenant_role_module_permissions(tenant_id, role);
```

---

## 4. JSONB Structures Reference

Before the tenant-scoped tables, here are the exact JSONB structures for all variable-content columns. Every field is documented with type and whether it is required.

### 4.1 Consultation Section Data (`consultation_sections.section_data`)

Each of the 9 consultation sections (A–I) stores its data as JSONB.

**Section A — Refracție / Acuitate Vizuală:**
```json
{
  "od": {
    "sph": -2.50,         // float | null
    "cyl": -0.75,         // float | null
    "axis": 90,           // int 0-180 | null
    "add": 2.00,          // float | null
    "va_sc": "6/18",      // string | null  (uncorrected VA)
    "va_cc": "6/6",       // string | null  (corrected VA)
    "bcva": "6/6",        // string | null  (best corrected VA)
    "seq": -2.875,        // float | null   (auto-computed: sph + cyl/2)
    "pd": 32.0            // float | null   (monocular PD in mm)
  },
  "os": { /* same structure as od */ },
  "pd_binocular": 64.0,   // float | null
  "notes": "..."          // string | null
}
```

**Section B — Biomicroscopie (Slit-lamp):**
```json
{
  "od": {
    "cornea": "transparentă",
    "ac": "normală profunzime",
    "iris": "normocromă",
    "cristalin": "transparent",
    "vitreous": null,
    "custom_findings": ""
  },
  "os": { /* same */ },
  "template_used": "Normal bilateral",   // string | null
  "notes": ""
}
```

**Section C — Examen fund de ochi (Fundoscopy):**
```json
{
  "od": {
    "disc": "roz, margini nete",
    "cup_disc_ratio": 0.3,   // float 0.0-1.0 | null
    "macula": "reflexiv prezent",
    "vessels": "normale",
    "periphery": "fara leziuni",
    "custom_findings": ""
  },
  "os": { /* same */ },
  "method": "DIRECT",   // "DIRECT" | "INDIRECT" | "SLIT_LAMP_78D" | "FUNDUS_CAMERA"
  "dilation_used": true,
  "dilation_agent": "Tropicamidă 1%",
  "template_used": null,
  "notes": ""
}
```

**Section D — Tonometrie (IOP):**
```json
{
  "od": {
    "iop": 14,         // int mmHg | null
    "method": "GAT",   // "GAT" | "NCT" | "ICARE" | "PASCAL"
    "time": "09:45",   // HH:MM | null
    "ccf_adjusted": null  // float | null (corneal correction factor adjusted value)
  },
  "os": { /* same */ },
  "cct_od": 545,        // int µm | null  (central corneal thickness)
  "cct_os": 542,        // int µm | null
  "alert_generated": false,
  "notes": ""
}
```

**Section E — Câmp Vizual:**
```json
{
  "od": {
    "investigation_id": "uuid | null",   // reference to investigations table
    "md": -2.1,                          // float dB | null (Mean Deviation)
    "psd": 1.8,                          // float dB | null (Pattern Std Deviation)
    "vfi": 97,                           // int % | null (Visual Field Index)
    "reliability": "OK",                 // string | null
    "interpretation": ""
  },
  "os": { /* same */ },
  "notes": ""
}
```

**Section F — Investigații speciale:**
```json
{
  "investigation_ids": ["uuid1", "uuid2"],   // array of investigation UUIDs
  "findings_summary": "",                    // free text
  "notes": ""
}
```

**Section G — Diagnostic:**
```json
{
  "primary_diagnosis": {
    "icd10_code": "H40.1",
    "icd10_name": "Glaucom cu unghi deschis",
    "laterality": "BILATERAL"    // "OD" | "OS" | "BILATERAL" | "N/A"
  },
  "secondary_diagnoses": [
    {
      "icd10_code": "H52.1",
      "icd10_name": "Miopie",
      "laterality": "BILATERAL"
    }
  ],
  "clinical_notes": "",
  "is_confirmed": true   // false = suspected/working diagnosis
}
```

**Section H — Plan de tratament:**
```json
{
  "medications": [
    {
      "name": "Latanoprost",
      "concentration": "0.005%",
      "form": "picături",
      "dosage": "1 picătură seara",
      "eye": "BILATERAL",   // "OD" | "OS" | "BILATERAL"
      "duration_days": 90,
      "has_surgical_risk_flag": true,
      "notes": ""
    }
  ],
  "procedures_planned": [],
  "referrals": [],
  "lifestyle_advice": "",
  "template_used": null,
  "notes": ""
}
```

**Section I — Recomandări:**
```json
{
  "next_appointment_weeks": 4,
  "recall_protocol_id": "uuid | null",
  "prescription_needed": true,
  "optical_order_needed": false,
  "investigations_ordered": [],
  "patient_instructions": "",
  "portal_message": "",      // visible to patient in portal
  "notes": ""
}
```

### 4.2 Investigation Result Data (`investigations.result_data`)

Structure varies per `category`. Examples:

**OCT (Optical Coherence Tomography):**
```json
{
  "device": "Heidelberg Spectralis",
  "protocol": "RNFL",
  "od": {
    "average_rnfl": 89,     // int µm
    "superior": 112,
    "inferior": 115,
    "temporal": 68,
    "nasal": 72,
    "gca_average": null,
    "interpretation": "normal"
  },
  "os": { /* same */ },
  "file_paths": ["investigation-images/uuid/oct_report.pdf"]
}
```

**Visual Field:**
```json
{
  "device": "Humphrey HFA3",
  "protocol": "24-2 SITA Standard",
  "od": { "md": -3.2, "psd": 2.1, "vfi": 94, "ghi": "Outside Normal Limits", "reliability_fixation_losses": "0/22", "file_path": "..." },
  "os": { /* same */ }
}
```

**Corneal Topography:**
```json
{
  "device": "Pentacam HR",
  "od": {
    "k1": 43.2, "k1_axis": 12,
    "k2": 44.8, "k2_axis": 102,
    "km": 44.0,
    "q_value": -0.23,
    "bai": 1.1,
    "amsler_coefficient": 0.89,
    "file_path": "..."
  },
  "os": { /* same */ }
}
```

### 4.3 Notification Rule Config (`notification_rules.config_data`)

```json
{
  "trigger_type": "RECALL",          // "RECALL" | "APPOINTMENT_REMINDER" | "PRESCRIPTION_EXPIRY" | "BIRTHDAY" | "CUSTOM"
  "recall_protocol_id": "uuid",      // if trigger_type = RECALL
  "timing_offsets_days": [-7, -1],   // days before event (negative = before)
  "channels": ["EMAIL", "SMS"],
  "template_email_subject": "Reamintire programare — {{clinic_name}}",
  "template_email_body": "Stimate/ă {{patient_name}}, ...",
  "template_sms": "Programare {{appointment_date}} ora {{appointment_time}}.",
  "is_active": true
}
```

---

## 5. Tenant-Scoped Tables — Module: patients

### 5.1 patients

```sql
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
    CONSTRAINT uq_patients_tenant_mrn UNIQUE (tenant_id, mrn)
);

CREATE INDEX idx_patients_tenant_id       ON patients(tenant_id);
CREATE INDEX idx_patients_last_name       ON patients(tenant_id, last_name);
CREATE INDEX idx_patients_dob             ON patients(tenant_id, date_of_birth);
CREATE INDEX idx_patients_phone           ON patients(tenant_id, phone);
CREATE INDEX idx_patients_email           ON patients(tenant_id, email);
CREATE INDEX idx_patients_full_name       ON patients
    USING gin(to_tsvector('simple', first_name || ' ' || last_name));
```

**MRN Generation:** The backend generates MRN as `OC-{6-digit-zero-padded-sequence-per-tenant}`.
Example: `OC-000001`, `OC-004821`. Sequence is per-tenant, stored in a dedicated column.

```sql
ALTER TABLE tenants ADD COLUMN mrn_sequence INT NOT NULL DEFAULT 0;
```

On each new patient creation: `UPDATE tenants SET mrn_sequence = mrn_sequence + 1 WHERE id = ?` (with row-level lock), then use the new value as the MRN suffix.

### 5.2 patient_medical_history

```sql
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
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pmh_patient UNIQUE (patient_id)
);

CREATE INDEX idx_pmh_tenant_patient ON patient_medical_history(tenant_id, patient_id);
```

### 5.3 patient_consents

```sql
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
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consents_patient ON patient_consents(tenant_id, patient_id);
CREATE INDEX idx_consents_type    ON patient_consents(tenant_id, consent_type);
```

### 5.4 patient_attachments

```sql
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
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachments_patient ON patient_attachments(tenant_id, patient_id);
```

---

## 6. Module: appointments

### 6.1 appointment_types

```sql
CREATE TABLE appointment_types (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    name            VARCHAR(128) NOT NULL,
    color_hex       VARCHAR(7)  NOT NULL DEFAULT '#13759C',  -- calendar block color
    duration_minutes INT        NOT NULL DEFAULT 30,
    description     TEXT,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_appt_types_tenant ON appointment_types(tenant_id);
```

### 6.2 appointments

```sql
CREATE TABLE appointments (
    id                  UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID                    NOT NULL REFERENCES tenants(id),
    patient_id          UUID                    NOT NULL REFERENCES patients(id),
    appointment_type_id UUID                    REFERENCES appointment_types(id),
    doctor_id           UUID                    NOT NULL,   -- staff member UUID (denormalized for query speed)
    doctor_name         VARCHAR(256)            NOT NULL,   -- denormalized snapshot
    start_at            TIMESTAMPTZ             NOT NULL,
    end_at              TIMESTAMPTZ             NOT NULL,
    status              appointment_status_type NOT NULL DEFAULT 'BOOKED',
    channel             appointment_channel_type NOT NULL DEFAULT 'IN_PERSON',
    room                VARCHAR(64),
    chief_complaint     TEXT,
    internal_notes      TEXT,
    patient_notes       TEXT,                   -- visible to patient in portal
    cancellation_reason TEXT,
    booked_by_id        UUID,                   -- who booked (staff or patient via portal)
    booked_via          VARCHAR(32) DEFAULT 'STAFF',  -- 'STAFF' | 'PORTAL' | 'PHONE'
    confirmed_at        TIMESTAMPTZ,
    checked_in_at       TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    no_show_at          TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    consultation_id     UUID,                   -- set when consultation is created (FK added below)
    created_at          TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_appointment_times CHECK (end_at > start_at)
);

CREATE INDEX idx_appointments_tenant_date   ON appointments(tenant_id, start_at);
CREATE INDEX idx_appointments_patient       ON appointments(tenant_id, patient_id);
CREATE INDEX idx_appointments_doctor        ON appointments(tenant_id, doctor_id, start_at);
CREATE INDEX idx_appointments_status        ON appointments(tenant_id, status);
```

### 6.3 blocked_slots

```sql
CREATE TABLE blocked_slots (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    doctor_id       UUID        NOT NULL,
    start_at        TIMESTAMPTZ NOT NULL,
    end_at          TIMESTAMPTZ NOT NULL,
    reason          VARCHAR(256),   -- 'Concediu', 'Congres', 'Blocat'
    is_recurring    BOOLEAN     DEFAULT FALSE,
    recurrence_rule VARCHAR(256),   -- iCal RRULE format if recurring
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_blocked_tenant_doctor ON blocked_slots(tenant_id, doctor_id, start_at);
```

---

## 7. Module: emr

### 7.1 consultations

```sql
CREATE TABLE consultations (
    id                  UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID                        NOT NULL REFERENCES tenants(id),
    patient_id          UUID                        NOT NULL REFERENCES patients(id),
    appointment_id      UUID                        REFERENCES appointments(id),
    doctor_id           UUID                        NOT NULL,
    doctor_name         VARCHAR(256)                NOT NULL,   -- snapshot at sign time
    doctor_signature    VARCHAR(512),               -- doctor's digital identifier
    status              consultation_status_type    NOT NULL DEFAULT 'DRAFT',
    consultation_date   DATE                        NOT NULL DEFAULT CURRENT_DATE,
    chief_complaint     TEXT,
    sections_completed  SMALLINT                    DEFAULT 0,  -- bitmask: sections A(1) B(2) C(4) D(8) E(16) F(32) G(64) H(128) I(256)
    signed_at           TIMESTAMPTZ,
    signed_by_id        UUID,
    created_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_consultation_appointment UNIQUE (appointment_id)   -- one consultation per appointment
);

CREATE INDEX idx_consultations_tenant_patient  ON consultations(tenant_id, patient_id);
CREATE INDEX idx_consultations_date            ON consultations(tenant_id, consultation_date DESC);
CREATE INDEX idx_consultations_doctor          ON consultations(tenant_id, doctor_id);
CREATE INDEX idx_consultations_status          ON consultations(tenant_id, status);
```

### 7.2 consultation_sections

```sql
CREATE TABLE consultation_sections (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    consultation_id UUID        NOT NULL REFERENCES consultations(id) ON DELETE CASCADE,
    section_code    CHAR(1)     NOT NULL,    -- 'A' through 'I'
    section_data    JSONB       NOT NULL DEFAULT '{}',
    is_completed    BOOLEAN     NOT NULL DEFAULT FALSE,
    completed_at    TIMESTAMPTZ,
    template_id     UUID,                    -- if section was pre-filled from a template
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_section_per_consultation UNIQUE (consultation_id, section_code),
    CONSTRAINT chk_section_code CHECK (section_code IN ('A','B','C','D','E','F','G','H','I'))
);

CREATE INDEX idx_sections_consultation ON consultation_sections(tenant_id, consultation_id);
-- GIN index for JSONB search (IOP queries, diagnosis queries)
CREATE INDEX idx_sections_data_gin     ON consultation_sections USING gin(section_data);
```

### 7.3 clinical_templates

```sql
CREATE TABLE clinical_templates (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    name            VARCHAR(256) NOT NULL,
    section_code    CHAR(1)     NOT NULL,
    template_data   JSONB       NOT NULL,    -- pre-fill values (same structure as section_data)
    category        VARCHAR(128),            -- 'Glaucom standard', 'Post-op cataractă', etc.
    is_global       BOOLEAN     DEFAULT FALSE,   -- shared across all doctors in tenant
    created_by_id   UUID,
    is_active       BOOLEAN     DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_templates_tenant_section ON clinical_templates(tenant_id, section_code);
```

---

## 8. Module: investigations

### 8.1 investigations

```sql
CREATE TABLE investigations (
    id                  UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID                        NOT NULL REFERENCES tenants(id),
    patient_id          UUID                        NOT NULL REFERENCES patients(id),
    consultation_id     UUID                        REFERENCES consultations(id),
    ordered_by_id       UUID                        NOT NULL,
    ordered_by_name     VARCHAR(256)                NOT NULL,
    category            investigation_category_type NOT NULL,
    name                VARCHAR(256)                NOT NULL,   -- e.g. "OCT RNFL OD+OS"
    device              VARCHAR(128),               -- device model used
    status              investigation_status_type   NOT NULL DEFAULT 'ORDERED',
    ordered_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    performed_at        TIMESTAMPTZ,
    result_data         JSONB,                      -- see Section 4.2
    interpretation      TEXT,                       -- clinician's text interpretation
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
```

### 8.2 investigation_files

```sql
CREATE TABLE investigation_files (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    investigation_id    UUID        NOT NULL REFERENCES investigations(id) ON DELETE CASCADE,
    file_name           VARCHAR(512) NOT NULL,
    storage_path        VARCHAR(1024) NOT NULL,
    mime_type           VARCHAR(128),
    file_size_bytes     BIGINT,
    file_type           VARCHAR(32),   -- 'DICOM', 'PDF', 'IMAGE', 'RAW_DATA'
    laterality          VARCHAR(8),    -- 'OD', 'OS', 'BOTH'
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inv_files_investigation ON investigation_files(tenant_id, investigation_id);
```

---

## 9. Module: prescriptions

### 9.1 prescriptions

```sql
CREATE TABLE prescriptions (
    id                  UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID                    NOT NULL REFERENCES tenants(id),
    patient_id          UUID                    NOT NULL REFERENCES patients(id),
    consultation_id     UUID                    REFERENCES consultations(id),
    prescription_number VARCHAR(32)             NOT NULL,    -- e.g. "RX-2026-004821"
    prescription_type   prescription_type       NOT NULL,
    status              prescription_status_type NOT NULL DEFAULT 'ACTIVE',
    issued_by_id        UUID                    NOT NULL,
    issued_by_name      VARCHAR(256)            NOT NULL,
    issued_at           TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    valid_from          DATE                    NOT NULL DEFAULT CURRENT_DATE,
    valid_until         DATE                    NOT NULL,
    pd_binocular        DECIMAL(5,1),            -- binocular PD mm
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
    superseded_by_id    UUID REFERENCES prescriptions(id),   -- points to newer Rx when status = SUPERSEDED
    qr_code_token       UUID            NOT NULL DEFAULT gen_random_uuid(),   -- for portal QR
    signed_at           TIMESTAMPTZ,
    pdf_path            VARCHAR(1024),   -- generated PDF in MinIO
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prescriptions_tenant_patient ON prescriptions(tenant_id, patient_id);
CREATE INDEX idx_prescriptions_status         ON prescriptions(tenant_id, status);
CREATE INDEX idx_prescriptions_valid_until    ON prescriptions(tenant_id, valid_until);
CREATE INDEX idx_prescriptions_qr_token       ON prescriptions(qr_code_token);
```

### 9.2 prescription_lines

```sql
CREATE TABLE prescription_lines (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    prescription_id     UUID        NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    eye                 CHAR(2)     NOT NULL,   -- 'OD' or 'OS'
    sph                 DECIMAL(5,2),
    cyl                 DECIMAL(5,2),
    axis                SMALLINT,               -- 0–180
    add_power           DECIMAL(5,2),
    va_sc               VARCHAR(16),            -- uncorrected VA e.g. "6/60"
    va_cc               VARCHAR(16),            -- corrected VA e.g. "6/6"
    bcva                VARCHAR(16),
    seq                 DECIMAL(5,2),           -- auto-computed, stored for audit
    CONSTRAINT chk_eye CHECK (eye IN ('OD', 'OS')),
    CONSTRAINT chk_axis_range CHECK (axis IS NULL OR (axis >= 0 AND axis <= 180))
);

CREATE INDEX idx_rx_lines_prescription ON prescription_lines(tenant_id, prescription_id);
```

---

## 10. Module: optical

### 10.1 service_catalog

```sql
CREATE TABLE service_catalog (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    name            VARCHAR(256) NOT NULL,
    category        VARCHAR(64) NOT NULL,    -- 'FRAME', 'LENS', 'CONTACT_LENS', 'ACCESSORY', 'SERVICE', 'CONSULTATION'
    sku             VARCHAR(64),
    brand           VARCHAR(128),
    unit_price      DECIMAL(10,2) NOT NULL DEFAULT 0,
    vat_rate        DECIMAL(5,2) NOT NULL DEFAULT 19.00,  -- % Romanian standard
    currency        CHAR(3) NOT NULL DEFAULT 'RON',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_service_catalog_tenant    ON service_catalog(tenant_id);
CREATE INDEX idx_service_catalog_category  ON service_catalog(tenant_id, category);
CREATE INDEX idx_service_catalog_sku       ON service_catalog(tenant_id, sku);
```

### 10.2 stock_items

```sql
CREATE TABLE stock_items (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    service_item_id UUID        REFERENCES service_catalog(id),
    name            VARCHAR(256) NOT NULL,
    category        VARCHAR(64) NOT NULL,
    sku             VARCHAR(64),
    barcode         VARCHAR(128),
    brand           VARCHAR(128),
    current_stock   INT         NOT NULL DEFAULT 0,
    minimum_stock   INT         NOT NULL DEFAULT 5,    -- alert threshold
    unit_cost       DECIMAL(10,2),
    unit_price      DECIMAL(10,2),
    currency        CHAR(3)     NOT NULL DEFAULT 'RON',
    location        VARCHAR(128),   -- physical location in clinic
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    last_restocked_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_stock_non_negative CHECK (current_stock >= 0)
);

CREATE INDEX idx_stock_tenant          ON stock_items(tenant_id);
CREATE INDEX idx_stock_low             ON stock_items(tenant_id, current_stock) WHERE current_stock <= minimum_stock;
CREATE INDEX idx_stock_sku             ON stock_items(tenant_id, sku);
```

### 10.3 optical_orders

```sql
CREATE TABLE optical_orders (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID            NOT NULL REFERENCES tenants(id),
    order_number        VARCHAR(32)     NOT NULL,   -- e.g. "CMD-2026-001234"
    patient_id          UUID            NOT NULL REFERENCES patients(id),
    prescription_id     UUID            REFERENCES prescriptions(id),
    consultation_id     UUID            REFERENCES consultations(id),
    order_type          order_type      NOT NULL DEFAULT 'GLASSES',
    stage               order_stage_type NOT NULL DEFAULT 'RECEIVED',
    assigned_to_id      UUID,           -- optical technician
    assigned_to_name    VARCHAR(256),
    lab_name            VARCHAR(256),
    lab_reference       VARCHAR(128),
    sent_to_lab_at      TIMESTAMPTZ,
    qc_passed_at        TIMESTAMPTZ,
    ready_at            TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    cancellation_reason TEXT,
    total_amount        DECIMAL(10,2)   NOT NULL DEFAULT 0,
    deposit_paid        DECIMAL(10,2)   NOT NULL DEFAULT 0,
    currency            CHAR(3)         NOT NULL DEFAULT 'RON',
    notes               TEXT,
    internal_notes      TEXT,
    created_by_id       UUID,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_optical_orders_number UNIQUE (tenant_id, order_number)
);

CREATE INDEX idx_optical_orders_tenant_patient ON optical_orders(tenant_id, patient_id);
CREATE INDEX idx_optical_orders_stage          ON optical_orders(tenant_id, stage);
CREATE INDEX idx_optical_orders_date           ON optical_orders(tenant_id, created_at DESC);
```

### 10.4 optical_order_items

```sql
CREATE TABLE optical_order_items (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    order_id            UUID        NOT NULL REFERENCES optical_orders(id) ON DELETE CASCADE,
    service_item_id     UUID        REFERENCES service_catalog(id),
    stock_item_id       UUID        REFERENCES stock_items(id),
    description         VARCHAR(512) NOT NULL,
    quantity            INT         NOT NULL DEFAULT 1,
    unit_price          DECIMAL(10,2) NOT NULL,
    discount_percent    DECIMAL(5,2) DEFAULT 0,
    line_total          DECIMAL(10,2) NOT NULL,    -- quantity * unit_price * (1 - discount/100)
    eye                 CHAR(2),                   -- 'OD', 'OS', or NULL for frame/accessory
    lens_specifications JSONB,                     -- sph, cyl, axis, add for lab order
    notes               TEXT
);

CREATE INDEX idx_order_items_order ON optical_order_items(tenant_id, order_id);
```

### 10.5 invoices

```sql
CREATE TABLE invoices (
    id                  UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID                NOT NULL REFERENCES tenants(id),
    invoice_number      VARCHAR(32)         NOT NULL,   -- e.g. "FC-2026-001234"
    patient_id          UUID                NOT NULL REFERENCES patients(id),
    optical_order_id    UUID                REFERENCES optical_orders(id),
    consultation_id     UUID                REFERENCES consultations(id),
    status              invoice_status_type NOT NULL DEFAULT 'DRAFT',
    issued_at           TIMESTAMPTZ,
    due_at              TIMESTAMPTZ,
    paid_at             TIMESTAMPTZ,
    subtotal            DECIMAL(10,2)       NOT NULL DEFAULT 0,
    vat_total           DECIMAL(10,2)       NOT NULL DEFAULT 0,
    discount_total      DECIMAL(10,2)       NOT NULL DEFAULT 0,
    total               DECIMAL(10,2)       NOT NULL DEFAULT 0,
    amount_paid         DECIMAL(10,2)       NOT NULL DEFAULT 0,
    currency            CHAR(3)             NOT NULL DEFAULT 'RON',
    payment_method      payment_method_type,
    payment_reference   VARCHAR(256),
    notes               TEXT,
    pdf_path            VARCHAR(1024),
    created_by_id       UUID,
    created_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_invoices_number UNIQUE (tenant_id, invoice_number)
);

CREATE INDEX idx_invoices_tenant_patient ON invoices(tenant_id, patient_id);
CREATE INDEX idx_invoices_status         ON invoices(tenant_id, status);
CREATE INDEX idx_invoices_date           ON invoices(tenant_id, issued_at DESC);
```

### 10.6 invoice_lines

```sql
CREATE TABLE invoice_lines (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    invoice_id          UUID        NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description         VARCHAR(512) NOT NULL,
    quantity            INT         NOT NULL DEFAULT 1,
    unit_price          DECIMAL(10,2) NOT NULL,
    vat_rate            DECIMAL(5,2) NOT NULL DEFAULT 19.00,
    discount_percent    DECIMAL(5,2) DEFAULT 0,
    line_total          DECIMAL(10,2) NOT NULL,
    service_item_id     UUID        REFERENCES service_catalog(id)
);

CREATE INDEX idx_invoice_lines_invoice ON invoice_lines(tenant_id, invoice_id);
```

---

## 11. Module: notifications

### 11.1 recall_protocols

```sql
CREATE TABLE recall_protocols (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    name                VARCHAR(256) NOT NULL,
    icd10_code          VARCHAR(16),             -- trigger diagnosis
    recall_interval_months INT      NOT NULL,    -- months between recalls
    description         TEXT,
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Default protocols seeded per tenant on creation:
-- Glaucom H40: every 3 months
-- AMD H35.3: every 6 months
-- Diabet retinopat H36.0: every 6 months
-- Miopie medie-mare H52.1 (> -3D): every 12 months
-- Keratoconus H18.6: every 6 months
-- Normal (preventiv): every 24 months
```

### 11.2 notification_rules

```sql
CREATE TABLE notification_rules (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    name            VARCHAR(256) NOT NULL,
    config_data     JSONB       NOT NULL,    -- see Section 4.3
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    last_run_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_rules_tenant ON notification_rules(tenant_id, is_active);
```

### 11.3 notification_log

```sql
CREATE TABLE notification_log (
    id                  UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID                        NOT NULL REFERENCES tenants(id),
    patient_id          UUID                        REFERENCES patients(id),
    rule_id             UUID                        REFERENCES notification_rules(id),
    channel             notification_channel_type   NOT NULL,
    status              notification_status_type    NOT NULL DEFAULT 'PENDING',
    recipient_address   VARCHAR(512)                NOT NULL,   -- email or phone
    subject             VARCHAR(512),
    body_preview        VARCHAR(512),               -- first 512 chars for audit display
    sent_at             TIMESTAMPTZ,
    failed_at           TIMESTAMPTZ,
    failure_reason      TEXT,
    external_message_id VARCHAR(256),               -- ID from SMS/email provider
    created_at          TIMESTAMPTZ                 NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_log_tenant_patient ON notification_log(tenant_id, patient_id);
CREATE INDEX idx_notif_log_status         ON notification_log(tenant_id, status);
CREATE INDEX idx_notif_log_date           ON notification_log(tenant_id, created_at DESC);
```

---

## 12. Module: admin

### 12.1 staff_members

```sql
CREATE TABLE staff_members (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID            NOT NULL REFERENCES tenants(id),
    keycloak_user_id    VARCHAR(256)    NOT NULL,   -- Keycloak user UUID
    first_name          VARCHAR(128)    NOT NULL,
    last_name           VARCHAR(128)    NOT NULL,
    email               VARCHAR(256)    NOT NULL,
    phone               VARCHAR(32),
    role                staff_role_type NOT NULL,
    specialization      VARCHAR(256),
    license_number      VARCHAR(128),   -- medical license
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    avatar_url          VARCHAR(1024),
    last_login_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_staff_tenant_keycloak UNIQUE (tenant_id, keycloak_user_id),
    CONSTRAINT uq_staff_tenant_email    UNIQUE (tenant_id, email)
);

CREATE INDEX idx_staff_tenant      ON staff_members(tenant_id);
CREATE INDEX idx_staff_tenant_role ON staff_members(tenant_id, role);
```

### 12.2 clinic_settings

```sql
CREATE TABLE clinic_settings (
    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID    UNIQUE NOT NULL REFERENCES tenants(id),   -- one row per tenant
    -- Working hours (JSONB: {mon: {open: "08:00", close: "18:00"}, ...})
    working_hours   JSONB   NOT NULL DEFAULT '{}',
    -- Appointment booking settings
    booking_advance_days    INT DEFAULT 90,   -- how far ahead can appointments be booked
    booking_slot_minutes    INT DEFAULT 15,   -- calendar slot granularity
    -- Invoice settings
    invoice_prefix          VARCHAR(16) DEFAULT 'FC',
    invoice_sequence        INT NOT NULL DEFAULT 0,
    order_number_prefix     VARCHAR(16) DEFAULT 'CMD',
    order_number_sequence   INT NOT NULL DEFAULT 0,
    prescription_prefix     VARCHAR(16) DEFAULT 'RX',
    prescription_sequence   INT NOT NULL DEFAULT 0,
    -- Communication settings
    sms_provider            VARCHAR(64),
    sms_api_key             TEXT,   -- encrypted at rest
    email_from              VARCHAR(256),
    email_provider          VARCHAR(64),
    email_api_key           TEXT,   -- encrypted at rest
    -- Portal settings
    portal_enabled          BOOLEAN DEFAULT TRUE,
    portal_appointment_booking BOOLEAN DEFAULT FALSE,
    -- Localization
    currency                CHAR(3) DEFAULT 'RON',
    vat_rate_default        DECIMAL(5,2) DEFAULT 19.00,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 13. Cross-Cutting: audit_log

This table is **append-only and immutable**. No UPDATE or DELETE is ever performed on it.
A PostgreSQL trigger prevents modifications:

```sql
CREATE TABLE audit_log (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL,   -- no FK — must survive tenant config changes
    user_id         UUID        NOT NULL,
    user_email      VARCHAR(256) NOT NULL,
    user_role       VARCHAR(64) NOT NULL,
    action          VARCHAR(64) NOT NULL,   -- 'CREATE', 'UPDATE', 'DELETE', 'VIEW', 'SIGN', 'EXPORT', 'LOGIN', 'LOGOUT'
    entity_type     VARCHAR(128) NOT NULL,  -- table name: 'patients', 'consultations', etc.
    entity_id       UUID,
    entity_snapshot JSONB,                  -- state of entity BEFORE the change (for UPDATE/DELETE)
    changed_fields  JSONB,                  -- array of changed field names (for UPDATE)
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    request_id      UUID,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_tenant_entity    ON audit_log(tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_tenant_user      ON audit_log(tenant_id, user_id);
CREATE INDEX idx_audit_tenant_date      ON audit_log(tenant_id, occurred_at DESC);
CREATE INDEX idx_audit_action           ON audit_log(tenant_id, action);

-- Immutability trigger
CREATE OR REPLACE FUNCTION audit_log_immutable()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is immutable: UPDATE and DELETE are not permitted';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_immutable
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_immutable();
```

---

## 14. Cross-Reference: Foreign Key Additions

The following FK constraints span modules and must be added after all tables are created:

```sql
-- appointments → consultations
ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_consultation
    FOREIGN KEY (consultation_id) REFERENCES consultations(id);

-- consultation_sections → clinical_templates
ALTER TABLE consultation_sections
    ADD CONSTRAINT fk_sections_template
    FOREIGN KEY (template_id) REFERENCES clinical_templates(id);

-- notification_log → recall_protocols (via rule)
-- Already covered by rule_id FK on notification_rules
```

---

## 15. Flyway Migration Strategy

### File Naming Convention

```
V{version}__{description}.sql

V1__baseline_schema.sql           ← enums + system tables + modules_catalog seed
V2__patients_module.sql           ← patients, patient_medical_history, consents, attachments
V3__appointments_module.sql       ← appointment_types, appointments, blocked_slots
V4__emr_module.sql                ← consultations, consultation_sections, clinical_templates
V5__investigations_module.sql     ← investigations, investigation_files
V6__prescriptions_module.sql      ← prescriptions, prescription_lines
V7__optical_module.sql            ← service_catalog, stock_items, optical_orders, order_items, invoices, invoice_lines
V8__notifications_module.sql      ← recall_protocols, notification_rules, notification_log
V9__admin_module.sql              ← staff_members, clinic_settings
V10__audit_module.sql             ← audit_log + immutability trigger
V11__cross_module_fks.sql         ← FK constraints that span modules
V12__seed_demo_tenant.sql         ← Demo tenant + demo patients (Section 16 below)
```

**Rules:**
- Always use two underscores (`__`) between version and description
- Description uses `_` (not `-`) between words
- Never edit a file that has been applied to any environment
- For schema changes post-V12: create `V13__`, `V14__`, etc.
- Repeatable migrations: prefix with `R__` (for views, stored procedures only)

---

## 16. Demo Seed Data (V12__seed_demo_tenant.sql)

This seed data creates the demo environment matching the approved Lovable prototype screenshots.

```sql
-- Demo Tenant
INSERT INTO tenants (id, slug, name, keycloak_realm, is_active) VALUES
    ('11111111-0000-0000-0000-000000000001',
     'clinica-demo',
     'Clinica Oftalmologică Demo SRL',
     'ophthacloud-demo',
     TRUE);

-- Demo Staff
INSERT INTO staff_members (id, tenant_id, keycloak_user_id, first_name, last_name, email, role) VALUES
    ('22222222-0000-0000-0000-000000000001',
     '11111111-0000-0000-0000-000000000001',
     'kc-doctor-ionescu',
     'Alexandru', 'Ionescu',
     'dr.ionescu@clinica-demo.ro',
     'DOCTOR'),
    ('22222222-0000-0000-0000-000000000002',
     '11111111-0000-0000-0000-000000000001',
     'kc-receptionist-maria',
     'Maria', 'Popescu',
     'maria.popescu@clinica-demo.ro',
     'RECEPTIONIST');

-- Demo Patients (matching prototype UI)
INSERT INTO patients (id, tenant_id, mrn, first_name, last_name, date_of_birth, gender, phone, email) VALUES
    ('33333333-0000-0000-0000-000000000001',
     '11111111-0000-0000-0000-000000000001',
     'OC-004821', 'Gheorghe', 'Ionescu',
     '1963-08-15', 'MALE', '0722 111 222', 'g.ionescu@email.ro'),

    ('33333333-0000-0000-0000-000000000002',
     '11111111-0000-0000-0000-000000000001',
     'OC-004822', 'Maria', 'Constantin',
     '1978-03-22', 'FEMALE', '0733 222 333', 'maria.c@email.ro'),

    ('33333333-0000-0000-0000-000000000003',
     '11111111-0000-0000-0000-000000000001',
     'OC-004823', 'Alexandru', 'Dima',
     '1990-11-05', 'MALE', '0744 333 444', 'alex.dima@email.ro'),

    ('33333333-0000-0000-0000-000000000004',
     '11111111-0000-0000-0000-000000000001',
     'OC-004824', 'Elena', 'Voicu',
     '1955-06-18', 'FEMALE', '0755 444 555', 'elena.v@email.ro'),

    ('33333333-0000-0000-0000-000000000005',
     '11111111-0000-0000-0000-000000000001',
     'OC-004825', 'Radu', 'Mihai',
     '1985-01-30', 'MALE', '0766 555 666', 'radu.m@email.ro');

-- Medical history for demo patients
INSERT INTO patient_medical_history (patient_id, tenant_id, has_glaucoma_history, active_diagnoses) VALUES
    ('33333333-0000-0000-0000-000000000001',
     '11111111-0000-0000-0000-000000000001',
     TRUE,
     '[{"icd10_code":"H40.1","icd10_name":"Glaucom cu unghi deschis","laterality":"BILATERAL"}]');

INSERT INTO patient_medical_history (patient_id, tenant_id, has_diabetes, active_diagnoses) VALUES
    ('33333333-0000-0000-0000-000000000004',
     '11111111-0000-0000-0000-000000000001',
     TRUE,
     '[{"icd10_code":"H36.0","icd10_name":"Retinopatie diabetică","laterality":"BILATERAL"}]');

-- Demo appointments for today
INSERT INTO appointments
    (id, tenant_id, patient_id, doctor_id, doctor_name, start_at, end_at, status, chief_complaint)
VALUES
    ('44444444-0000-0000-0000-000000000001',
     '11111111-0000-0000-0000-000000000001',
     '33333333-0000-0000-0000-000000000001',
     '22222222-0000-0000-0000-000000000001',
     'Dr. Alexandru Ionescu',
     NOW()::DATE + INTERVAL '9 hours',
     NOW()::DATE + INTERVAL '9 hours 30 minutes',
     'CHECKED_IN',
     'Control glaucom — vedere încetoșată matinal'),

    ('44444444-0000-0000-0000-000000000002',
     '11111111-0000-0000-0000-000000000001',
     '33333333-0000-0000-0000-000000000002',
     '22222222-0000-0000-0000-000000000001',
     'Dr. Alexandru Ionescu',
     NOW()::DATE + INTERVAL '10 hours',
     NOW()::DATE + INTERVAL '10 hours 30 minutes',
     'CONFIRMED',
     'Consultație inițială — prescripție ochelari'),

    ('44444444-0000-0000-0000-000000000003',
     '11111111-0000-0000-0000-000000000001',
     '33333333-0000-0000-0000-000000000003',
     '22222222-0000-0000-0000-000000000001',
     'Dr. Alexandru Ionescu',
     NOW()::DATE + INTERVAL '11 hours',
     NOW()::DATE + INTERVAL '11 hours 30 minutes',
     'BOOKED',
     'Miopie progresivă — control anual'),

    ('44444444-0000-0000-0000-000000000004',
     '11111111-0000-0000-0000-000000000001',
     '33333333-0000-0000-0000-000000000004',
     '22222222-0000-0000-0000-000000000001',
     'Dr. Alexandru Ionescu',
     NOW()::DATE + INTERVAL '14 hours',
     NOW()::DATE + INTERVAL '14 hours 30 minutes',
     'BOOKED',
     'Control retinopatie diabetică'),

    ('44444444-0000-0000-0000-000000000005',
     '11111111-0000-0000-0000-000000000001',
     '33333333-0000-0000-0000-000000000005',
     '22222222-0000-0000-0000-000000000001',
     'Dr. Alexandru Ionescu',
     NOW()::DATE + INTERVAL '15 hours',
     NOW()::DATE + INTERVAL '15 hours 30 minutes',
     'BOOKED',
     'Astigmatism — prescripție lentile de contact');

-- Default recall protocols
INSERT INTO recall_protocols (tenant_id, name, icd10_code, recall_interval_months) VALUES
    ('11111111-0000-0000-0000-000000000001', 'Control Glaucom',         'H40',  3),
    ('11111111-0000-0000-0000-000000000001', 'Control AMD',             'H35.3',6),
    ('11111111-0000-0000-0000-000000000001', 'Control Retinopatie DZ',  'H36.0',6),
    ('11111111-0000-0000-0000-000000000001', 'Control Miopie',          'H52.1',12),
    ('11111111-0000-0000-0000-000000000001', 'Control Keratoconus',     'H18.6',6),
    ('11111111-0000-0000-0000-000000000001', 'Examen Preventiv',        NULL,   24);

-- Default clinic settings
INSERT INTO clinic_settings (tenant_id, working_hours, booking_slot_minutes) VALUES
    ('11111111-0000-0000-0000-000000000001',
     '{"mon":{"open":"08:00","close":"18:00"},"tue":{"open":"08:00","close":"18:00"},"wed":{"open":"08:00","close":"18:00"},"thu":{"open":"08:00","close":"18:00"},"fri":{"open":"08:00","close":"16:00"},"sat":{"open":"09:00","close":"13:00"},"sun":null}',
     15);
```

---

## 17. Indexes Summary

Critical indexes for production performance (already included in table definitions above, summarized here for review):

| Table | Index | Use Case |
|---|---|---|
| `patients` | `idx_patients_full_name` (GIN tsvector) | Full-text search by name |
| `patients` | `idx_patients_last_name` | Sorted patient list |
| `appointments` | `idx_appointments_tenant_date` | Daily agenda, calendar view |
| `appointments` | `idx_appointments_doctor` | Doctor's schedule |
| `consultations` | `idx_consultations_date DESC` | Patient history timeline |
| `consultation_sections` | `idx_sections_data_gin` (GIN JSONB) | IOP queries, diagnosis search |
| `audit_log` | `idx_audit_tenant_date DESC` | Audit log viewer |
| `stock_items` | `idx_stock_low` (partial index) | Low-stock dashboard widget |
| `prescriptions` | `idx_prescriptions_valid_until` | Expiry recall jobs |
| `notification_log` | `idx_notif_log_status` | Pending notification queue |

---

*End of GUIDE_03 — Data Model*  
*Next document: GUIDE_04 — API Contract*
