-- V10__fix_appointments_enum_types.sql
-- Spring Boot 4 / Hibernate 7 with PostgreSQL JDBC driver does NOT support
-- implicit varchar → native enum casts in parameterized prepared statements.
-- Replace appointment_status_type and appointment_channel_type columns with
-- plain VARCHAR(32), which is fully compatible with @Enumerated(EnumType.STRING)
-- and avoids the "column is of type X but expression is of type character varying" error.

ALTER TABLE appointments
    ALTER COLUMN status  TYPE VARCHAR(32) USING status::TEXT,
    ALTER COLUMN channel TYPE VARCHAR(32) USING channel::TEXT;
