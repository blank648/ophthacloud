-- V15: Ensure prescription enum types exist and fix column type compatibility.
-- Converts PostgreSQL named enum columns to VARCHAR so Hibernate's standard
-- @Enumerated(EnumType.STRING) mapping works without @JdbcTypeCode(NAMED_ENUM).

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'prescription_status_type') THEN
        CREATE TYPE prescription_status_type AS ENUM (
            'ACTIVE', 'SIGNED', 'EXPIRED', 'CANCELLED', 'SUPERSEDED'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'prescription_type') THEN
        CREATE TYPE prescription_type AS ENUM (
            'DISTANCE', 'NEAR', 'PROGRESSIVE', 'BIFOCAL', 'CONTACT_LENS', 'OCCUPATIONAL'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lens_type') THEN
        CREATE TYPE lens_type AS ENUM (
            'SINGLE_VISION', 'BIFOCAL', 'PROGRESSIVE', 'TRIFOCAL',
            'CONTACT_SOFT', 'CONTACT_RGP', 'CONTACT_SCLERAL'
        );
    END IF;
END $$;

-- Convert prescription enum columns from PG named enum → VARCHAR(64)
-- so Hibernate's @Enumerated(EnumType.STRING) works without @JdbcTypeCode(NAMED_ENUM).
-- Constraint values are preserved; only the storage type changes.

DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'prescriptions'
          AND column_name = 'status'
          AND udt_name = 'prescription_status_type'
    ) THEN
        ALTER TABLE prescriptions
            ALTER COLUMN status          TYPE VARCHAR(64) USING status::text,
            ALTER COLUMN prescription_type TYPE VARCHAR(64) USING prescription_type::text,
            ALTER COLUMN lens_type        TYPE VARCHAR(64) USING lens_type::text;
    END IF;
END $$;

