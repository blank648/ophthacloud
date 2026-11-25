CREATE TABLE adherence_logs (
    id            UUID        PRIMARY KEY,
    plan_id       UUID        NOT NULL REFERENCES treatment_plans(id) ON DELETE RESTRICT,
    scheduled_for TIMESTAMP   NOT NULL,
    confirmed_at  TIMESTAMP,
    status        VARCHAR(20) NOT NULL,

    CONSTRAINT uk_adherence_logs_plan_scheduled UNIQUE (plan_id, scheduled_for),
    CONSTRAINT chk_adherence_logs_status CHECK (status IN ('PENDING', 'CONFIRMED_ON_TIME', 'CONFIRMED_LATE', 'MISSED')),
    CONSTRAINT chk_adherence_logs_confirmed_at CHECK (
        (status IN ('CONFIRMED_ON_TIME', 'CONFIRMED_LATE') AND confirmed_at IS NOT NULL)
        OR (status IN ('PENDING', 'MISSED') AND confirmed_at IS NULL)
    )
);

CREATE TABLE amsler_tests (
    id                    UUID        PRIMARY KEY,
    patient_id            UUID        NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    tested_eye            CHAR(2)     NOT NULL,
    distorted_cells       JSONB       NOT NULL,
    symptom_type          VARCHAR(30),
    severity              VARCHAR(24) NOT NULL,
    alert_acknowledged_by UUID        REFERENCES users(id) ON DELETE RESTRICT,
    tested_at             TIMESTAMP   NOT NULL,

    CONSTRAINT chk_amsler_tests_eye CHECK (tested_eye IN ('OD', 'OS')),
    CONSTRAINT chk_amsler_tests_cells CHECK (jsonb_typeof(distorted_cells) = 'array'),
    CONSTRAINT chk_amsler_tests_severity CHECK (severity IN ('NORMAL_FARA_DISTORSIUNI', 'MODERATA_MONITORIZARE', 'CRITICA_URGENTA')),
    CONSTRAINT chk_amsler_tests_normal_ack CHECK (severity <> 'NORMAL_FARA_DISTORSIUNI' OR alert_acknowledged_by IS NULL)
);

CREATE INDEX idx_amsler_tests_patient_tested_at ON amsler_tests (patient_id, tested_at);
CREATE INDEX idx_amsler_tests_open_alerts
    ON amsler_tests (severity, tested_at)
    WHERE severity <> 'NORMAL_FARA_DISTORSIUNI' AND alert_acknowledged_by IS NULL;
