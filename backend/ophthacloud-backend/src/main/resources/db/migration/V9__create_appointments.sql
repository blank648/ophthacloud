-- V9__create_appointments.sql
-- Appointments module: appointment_types, appointments, blocked_slots
-- Per GUIDE_03 §6

-- ── appointment_types ─────────────────────────────────────────────────────────
CREATE TABLE appointment_types (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL REFERENCES tenants(id),
    name             VARCHAR(128) NOT NULL,
    color_hex        VARCHAR(7)   NOT NULL DEFAULT '#13759C',
    duration_minutes INT          NOT NULL DEFAULT 30,
    description      TEXT,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_appt_types_tenant ON appointment_types(tenant_id);

-- ── appointments ──────────────────────────────────────────────────────────────
CREATE TABLE appointments (
    id                   UUID                      PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID                      NOT NULL REFERENCES tenants(id),
    patient_id           UUID                      NOT NULL REFERENCES patients(id),
    appointment_type_id  UUID                      REFERENCES appointment_types(id),
    doctor_id            UUID                      NOT NULL,
    doctor_name          VARCHAR(256)              NOT NULL,
    start_at             TIMESTAMPTZ               NOT NULL,
    end_at               TIMESTAMPTZ               NOT NULL,
    duration_minutes     INT                       NOT NULL DEFAULT 30,
    status               appointment_status_type   NOT NULL DEFAULT 'BOOKED',
    channel              appointment_channel_type  NOT NULL DEFAULT 'IN_PERSON',
    room                 VARCHAR(64),
    chief_complaint      TEXT,
    internal_notes       TEXT,
    patient_notes        TEXT,
    cancellation_reason  TEXT,
    booked_by_id         UUID,
    booked_via           VARCHAR(32)               DEFAULT 'STAFF',
    confirmed_at         TIMESTAMPTZ,
    checked_in_at        TIMESTAMPTZ,
    completed_at         TIMESTAMPTZ,
    no_show_at           TIMESTAMPTZ,
    cancelled_at         TIMESTAMPTZ,
    consultation_id      UUID,                     -- FK added after consultation table exists
    created_at           TIMESTAMPTZ               NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ               NOT NULL DEFAULT NOW(),
    version              BIGINT                    NOT NULL DEFAULT 0,
    CONSTRAINT chk_appointment_times CHECK (end_at > start_at)
);

CREATE INDEX idx_appointments_tenant_date ON appointments(tenant_id, start_at);
CREATE INDEX idx_appointments_patient     ON appointments(tenant_id, patient_id);
CREATE INDEX idx_appointments_doctor      ON appointments(tenant_id, doctor_id, start_at);
CREATE INDEX idx_appointments_status      ON appointments(tenant_id, status);

-- ── blocked_slots ─────────────────────────────────────────────────────────────
CREATE TABLE blocked_slots (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID        NOT NULL REFERENCES tenants(id),
    doctor_id        UUID        NOT NULL,
    start_at         TIMESTAMPTZ NOT NULL,
    end_at           TIMESTAMPTZ NOT NULL,
    reason           VARCHAR(256),
    is_recurring     BOOLEAN     DEFAULT FALSE,
    recurrence_rule  VARCHAR(256),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version          BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_blocked_tenant_doctor ON blocked_slots(tenant_id, doctor_id, start_at);
