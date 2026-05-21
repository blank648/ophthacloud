-- V22: Fix admin module tables for Hibernate BaseEntity compatibility

-- 1. Add version column for optimistic locking (required by BaseEntity)
ALTER TABLE tenant_role_module_permissions
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

ALTER TABLE staff_members
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

ALTER TABLE clinic_settings
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- 2. Convert tenant_role_module_permissions.role from native staff_role_type enum to VARCHAR(64)
-- to avoid Hibernate @Enumerated(EnumType.STRING) mapping friction
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tenant_role_module_permissions'
          AND column_name = 'role'
          AND udt_name = 'staff_role_type'
    ) THEN
        ALTER TABLE tenant_role_module_permissions
            ALTER COLUMN role TYPE VARCHAR(64) USING role::text;
    END IF;
END $$;

-- 3. Convert clinic_settings.currency from CHAR(3) (bpchar) to VARCHAR(3)
-- Hibernate schema validation expects varchar, not bpchar
ALTER TABLE clinic_settings
    ALTER COLUMN currency TYPE VARCHAR(3);
