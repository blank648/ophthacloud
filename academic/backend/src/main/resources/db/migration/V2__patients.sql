CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE patients (
    id            UUID         PRIMARY KEY,
    user_id       UUID         NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    cnp           CHAR(13)     NOT NULL UNIQUE,
    last_name     VARCHAR(80)  NOT NULL,
    first_name    VARCHAR(80)  NOT NULL,
    phone         VARCHAR(20),
    birth_date    DATE         NOT NULL,
    sex           VARCHAR(16)  NOT NULL,
    ui_font_scale NUMERIC(2,1) NOT NULL DEFAULT 1.0,
    high_contrast BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_patients_cnp CHECK (cnp ~ '^[0-9]{13}$'),
    CONSTRAINT chk_patients_ui_font_scale CHECK (ui_font_scale BETWEEN 1.0 AND 2.0)
);

CREATE INDEX idx_patients_name_lower ON patients (LOWER(last_name), LOWER(first_name));
CREATE INDEX idx_patients_phone ON patients (phone);
CREATE INDEX idx_patients_name_trgm ON patients USING gin ((LOWER(last_name || ' ' || first_name)) gin_trgm_ops);
