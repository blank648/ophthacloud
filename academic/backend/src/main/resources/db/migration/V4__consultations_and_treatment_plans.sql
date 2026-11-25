CREATE TABLE consultations (
    id                 UUID         PRIMARY KEY,
    appointment_id     UUID         NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE RESTRICT,
    va_od              VARCHAR(16)  NOT NULL,
    va_os              VARCHAR(16)  NOT NULL,
    iop_od             NUMERIC(4,1) NOT NULL,
    iop_os             NUMERIC(4,1) NOT NULL,
    biomicroscopy_notes TEXT,
    diagnosis_icd10    VARCHAR(16)  NOT NULL,

    CONSTRAINT chk_consultations_iop_od CHECK (iop_od >= 0),
    CONSTRAINT chk_consultations_iop_os CHECK (iop_os >= 0)
);

CREATE TABLE treatment_plans (
    id              UUID         PRIMARY KEY,
    consultation_id UUID         NOT NULL REFERENCES consultations(id) ON DELETE RESTRICT,
    patient_id      UUID         NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    medication_name VARCHAR(120) NOT NULL,
    target_eye      CHAR(2)      NOT NULL,
    drops_per_dose  INT          NOT NULL,
    schedule_hours  JSONB        NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    activated_at    TIMESTAMP    NOT NULL,
    stopped_at      TIMESTAMP,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_treatment_plans_eye CHECK (target_eye IN ('OD', 'OS', 'AO')),
    CONSTRAINT chk_treatment_plans_drops CHECK (drops_per_dose > 0),
    CONSTRAINT chk_treatment_plans_schedule_hours CHECK (
        CASE
            WHEN jsonb_typeof(schedule_hours) = 'array' THEN jsonb_array_length(schedule_hours) > 0
            ELSE FALSE
        END
    ),
    CONSTRAINT chk_treatment_plans_dates CHECK (start_date <= end_date),
    CONSTRAINT chk_treatment_plans_stopped_at CHECK (stopped_at IS NULL OR stopped_at >= activated_at)
);

CREATE OR REPLACE FUNCTION fn_verify_treatment_plan_patient()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_appointment_patient_id UUID;
BEGIN
    SELECT a.patient_id
      INTO v_appointment_patient_id
      FROM consultations c
      JOIN appointments a ON a.id = c.appointment_id
     WHERE c.id = NEW.consultation_id;

    IF v_appointment_patient_id IS NULL THEN
        RAISE EXCEPTION 'Consultation % does not exist or has no linked appointment', NEW.consultation_id
            USING ERRCODE = 'foreign_key_violation';
    END IF;

    IF NEW.patient_id <> v_appointment_patient_id THEN
        RAISE EXCEPTION 'treatment_plans.patient_id (%) does not match appointments.patient_id (%) for consultation %',
            NEW.patient_id, v_appointment_patient_id, NEW.consultation_id
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_treatment_plans_patient_consistency
    BEFORE INSERT OR UPDATE OF consultation_id, patient_id ON treatment_plans
    FOR EACH ROW
    EXECUTE FUNCTION fn_verify_treatment_plan_patient();

CREATE INDEX idx_treatment_plans_consultation ON treatment_plans (consultation_id);
CREATE INDEX idx_treatment_plans_patient_dates ON treatment_plans (patient_id, start_date, end_date);
