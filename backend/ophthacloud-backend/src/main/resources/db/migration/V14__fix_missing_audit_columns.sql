-- V14: Fix missing enum types, audit columns, and type mismatches from V12/V13.
-- updated_at and version were omitted from investigation_files in V12.
-- version was omitted from investigations, prescriptions, and prescription_lines.
-- created_at and updated_at were omitted from prescription_lines in V13.
-- eye column was CHAR(2) in V13 but Hibernate entity maps it as VARCHAR(2).
-- Prescription enum types were referenced in V13 but never CREATE'd.

-- ── Missing PostgreSQL enum types (referenced in V13 table definitions) ──────

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

-- ── Audit columns & type fixes ────────────────────────────────────────────────

ALTER TABLE investigation_files
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE investigations
    ADD COLUMN IF NOT EXISTS version BIGINT;

ALTER TABLE prescriptions
    ADD COLUMN IF NOT EXISTS version BIGINT;

ALTER TABLE prescription_lines
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS version    BIGINT;

-- Fix eye column type: CHAR(2) → VARCHAR(2) to match JPA mapping
ALTER TABLE prescription_lines
    ALTER COLUMN eye TYPE VARCHAR(2);

