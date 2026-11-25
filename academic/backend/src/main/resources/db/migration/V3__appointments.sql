CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE appointments (
    id                 UUID        PRIMARY KEY,
    patient_id         UUID        NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    doctor_id          UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    scheduled_at       TIMESTAMP   NOT NULL,
    duration_min       INT         NOT NULL DEFAULT 30,
    status             VARCHAR(20) NOT NULL,
    applied_voucher_id UUID        UNIQUE,

    CONSTRAINT uk_appointments_doctor_scheduled UNIQUE (doctor_id, scheduled_at),
    CONSTRAINT chk_appointments_duration CHECK (duration_min > 0),
    CONSTRAINT chk_appointments_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'NO_SHOW')),
    CONSTRAINT excl_appointments_doctor_overlap EXCLUDE USING gist (
        doctor_id WITH =,
        tsrange(scheduled_at, scheduled_at + (duration_min * INTERVAL '1 minute'), '[)') WITH &&
    )
);

CREATE INDEX idx_appointments_patient_scheduled ON appointments (patient_id, scheduled_at);
