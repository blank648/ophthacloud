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

-- 3.1 tenants
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
    mrn_sequence    INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenants_slug ON tenants(slug);
CREATE INDEX idx_tenants_keycloak_realm ON tenants(keycloak_realm);

-- 3.2 modules_catalog
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

-- 3.3 tenant_role_module_permissions
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
