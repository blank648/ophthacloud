-- V34__fix_tenant_name_and_admin_keycloak_id.sql
-- Fixes two data inconsistencies discovered during login debugging:
--
-- 1. Tenant name was "Clinica Oftalmologică Demo SRL" (seed default) but the
--    clinic owner had already renamed it to "Clinica Oftalmologică Ocheada" via
--    the Settings page. The Settings page correctly writes to the tenants table,
--    so this migration just documents the expected value. If the name was already
--    updated by the user via UI, the ON CONFLICT / WHERE clause makes this a no-op.
--
-- 2. The CLINIC_ADMIN staff record (id=22222222-0000-0000-0000-000000000002) was
--    seeded with a placeholder keycloak_user_id (ef416065-ebd1-4317-bbfd-349586c665e3)
--    that does not match the actual Keycloak user UUID (8fafbd91-f95b-49b3-be2f-96acc87748bf)
--    for admin.test@clinica-demo.ro. This mismatch caused the ProfileController's
--    Keycloak email-sync call to look up the wrong user (and fail silently).
--
-- 3. The CLINIC_ADMIN staff name was "System Admin" (seed placeholder). Updating
--    to a more appropriate display name.

-- Fix clinic name (only if it still shows the old seed value)
UPDATE tenants
SET name = 'Clinica Oftalmologică Ocheada'
WHERE id = '11111111-0000-0000-0000-000000000001'
  AND name = 'Clinica Oftalmologică Demo SRL';

-- Fix the admin staff member's keycloak_user_id to match the actual Keycloak UUID
UPDATE staff_members
SET keycloak_user_id = '8fafbd91-f95b-49b3-be2f-96acc87748bf',
    email            = 'admin.test@clinica-demo.ro',
    first_name       = 'Admin',
    last_name        = 'Clinică'
WHERE id = '22222222-0000-0000-0000-000000000002'
  AND tenant_id = '11111111-0000-0000-0000-000000000001';
