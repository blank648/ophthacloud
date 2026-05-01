-- V17: Convert gender_type ENUM to VARCHAR(32) in patients table
-- This avoids Hibernate/PostgreSQL mapping friction consistent with V10, V15, and V16.

-- 1. Change the column type (PostgreSQL automatically casts ENUM to VARCHAR)
ALTER TABLE patients
    ALTER COLUMN gender TYPE VARCHAR(32);

-- 2. Optional: We could drop the gender_type ENUM if no longer used,
-- but keeping it for now to avoid breaking other potential references.
