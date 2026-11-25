CREATE TABLE gamification_profiles (
    patient_id          UUID  PRIMARY KEY REFERENCES patients(id) ON DELETE RESTRICT,
    current_streak      INT   NOT NULL DEFAULT 0,
    best_streak         INT   NOT NULL DEFAULT 0,
    forgiveness_shields INT   NOT NULL DEFAULT 0,
    total_points        INT   NOT NULL DEFAULT 0,
    unlocked_badges     JSONB NOT NULL DEFAULT '[]'::jsonb,
    last_completed_date DATE,

    CONSTRAINT chk_gamification_current_streak CHECK (current_streak >= 0),
    CONSTRAINT chk_gamification_best_streak CHECK (best_streak >= current_streak),
    CONSTRAINT chk_gamification_shields CHECK (forgiveness_shields BETWEEN 0 AND 2),
    CONSTRAINT chk_gamification_points CHECK (total_points >= 0),
    CONSTRAINT chk_gamification_badges CHECK (jsonb_typeof(unlocked_badges) = 'array')
);

CREATE TABLE discount_vouchers (
    id               UUID        PRIMARY KEY,
    patient_id       UUID        NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    voucher_code     VARCHAR(24) NOT NULL UNIQUE,
    discount_percent INT         NOT NULL,
    points_milestone INT         NOT NULL,
    status           VARCHAR(16) NOT NULL,
    expires_at       TIMESTAMP   NOT NULL,
    redeemed_at      TIMESTAMP,

    CONSTRAINT uk_discount_vouchers_patient_milestone UNIQUE (patient_id, points_milestone),
    CONSTRAINT chk_discount_vouchers_percent CHECK (discount_percent IN (5, 10, 15)),
    CONSTRAINT chk_discount_vouchers_milestone CHECK (points_milestone > 0),
    CONSTRAINT chk_discount_vouchers_status CHECK (status IN ('ACTIVE', 'REDEEMED', 'EXPIRED')),
    CONSTRAINT chk_discount_vouchers_redeemed_at CHECK (
        (status = 'REDEEMED' AND redeemed_at IS NOT NULL)
        OR (status <> 'REDEEMED' AND redeemed_at IS NULL)
    )
);

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_voucher
        FOREIGN KEY (applied_voucher_id) REFERENCES discount_vouchers(id) ON DELETE RESTRICT;

CREATE OR REPLACE FUNCTION fn_verify_appointment_voucher_owner()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_voucher_patient_id UUID;
BEGIN
    SELECT dv.patient_id
      INTO v_voucher_patient_id
      FROM discount_vouchers dv
     WHERE dv.id = NEW.applied_voucher_id;

    IF v_voucher_patient_id IS NULL THEN
        RAISE EXCEPTION 'Voucher % does not exist', NEW.applied_voucher_id
            USING ERRCODE = 'foreign_key_violation';
    END IF;

    IF NEW.patient_id <> v_voucher_patient_id THEN
        RAISE EXCEPTION 'Voucher % belongs to patient %, not appointment patient %',
            NEW.applied_voucher_id, v_voucher_patient_id, NEW.patient_id
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_appointments_voucher_owner
    BEFORE INSERT OR UPDATE OF applied_voucher_id, patient_id ON appointments
    FOR EACH ROW
    WHEN (NEW.applied_voucher_id IS NOT NULL)
    EXECUTE FUNCTION fn_verify_appointment_voucher_owner();
