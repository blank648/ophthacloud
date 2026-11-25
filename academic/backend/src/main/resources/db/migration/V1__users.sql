CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_users_email_normalized CHECK (email = LOWER(BTRIM(email)) AND BTRIM(email) <> ''),
    CONSTRAINT chk_users_role CHECK (role IN ('PATIENT', 'DOCTOR', 'RECEPTIONIST', 'ADMIN'))
);
