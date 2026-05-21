-- Create staff_members table
CREATE TABLE staff_members (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID            NOT NULL REFERENCES tenants(id),
    keycloak_user_id    VARCHAR(256)    NOT NULL,
    first_name          VARCHAR(128)    NOT NULL,
    last_name           VARCHAR(128)    NOT NULL,
    email               VARCHAR(256)    NOT NULL,
    phone               VARCHAR(32),
    role                VARCHAR(64)     NOT NULL, -- Using VARCHAR(64) to avoid Hibernate enum mapping friction
    specialization      VARCHAR(256),
    license_number      VARCHAR(128),
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

-- Create clinic_settings table
CREATE TABLE clinic_settings (
    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID    UNIQUE NOT NULL REFERENCES tenants(id),
    working_hours   JSONB   NOT NULL DEFAULT '{}',
    booking_advance_days    INT DEFAULT 90,
    booking_slot_minutes    INT DEFAULT 15,
    invoice_prefix          VARCHAR(16) DEFAULT 'FC',
    invoice_sequence        INT NOT NULL DEFAULT 0,
    order_number_prefix     VARCHAR(16) DEFAULT 'CMD',
    order_number_sequence   INT NOT NULL DEFAULT 0,
    prescription_prefix     VARCHAR(16) DEFAULT 'RX',
    prescription_sequence   INT NOT NULL DEFAULT 0,
    sms_provider            VARCHAR(64),
    sms_api_key             TEXT,
    email_from              VARCHAR(256),
    email_provider          VARCHAR(64),
    email_api_key           TEXT,
    portal_enabled          BOOLEAN DEFAULT TRUE,
    portal_appointment_booking BOOLEAN DEFAULT FALSE,
    currency                CHAR(3) DEFAULT 'RON',
    vat_rate_default        DECIMAL(5,2) DEFAULT 19.00,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
