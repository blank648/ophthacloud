-- V16: Fix investigation enum types for Hibernate compatibility

-- 1. Drop existing defaults that depend on the enum type
ALTER TABLE investigations
    ALTER COLUMN status DROP DEFAULT;

-- 2. Alter columns to VARCHAR
ALTER TABLE investigations
    ALTER COLUMN category TYPE VARCHAR(64) USING category::text,
    ALTER COLUMN status TYPE VARCHAR(64) USING status::text;

-- 3. Restore the default using VARCHAR
ALTER TABLE investigations
    ALTER COLUMN status SET DEFAULT 'ORDERED';

-- 4. Safely drop the old enum types
DROP TYPE IF EXISTS investigation_category_type CASCADE;
DROP TYPE IF EXISTS investigation_status_type CASCADE;
