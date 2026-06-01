-- V24__seed_demo_tenant.sql
-- Seed demo tenant, staff, patients, appointments, recalls, and settings

-- 1. Demo Tenant
INSERT INTO tenants (id, slug, name, keycloak_realm, is_active, mrn_sequence) VALUES
    ('11111111-0000-0000-0000-000000000001',
     'clinica-demo',
     'Clinica Oftalmologică Demo SRL',
     'ophthacloud-demo',
     TRUE,
     4825)
ON CONFLICT (id) DO NOTHING;

-- 2. Demo Staff
INSERT INTO staff_members (id, tenant_id, keycloak_user_id, first_name, last_name, email, role, is_active) VALUES
    ('22222222-0000-0000-0000-000000000002',
     '11111111-0000-0000-0000-000000000001',
     'ef416065-ebd1-4317-bbfd-349586c665e3',
     'System', 'Admin',
     'admin@ophthacloud.ro',
     'CLINIC_ADMIN',
     TRUE)
ON CONFLICT (id) DO NOTHING;

-- Data cleared for production

-- 6. Default recall protocols
INSERT INTO recall_protocols (id, tenant_id, name, icd10_code, recall_interval_months, is_active) VALUES
    ('55555555-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', 'Control Glaucom',         'H40',  3, TRUE),
    ('55555555-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000001', 'Control AMD',             'H35.3',6, TRUE),
    ('55555555-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000001', 'Control Retinopatie DZ',  'H36.0',6, TRUE),
    ('55555555-0000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000001', 'Control Miopie',          'H52.1',12, TRUE),
    ('55555555-0000-0000-0000-000000000005', '11111111-0000-0000-0000-000000000001', 'Control Keratoconus',     'H18.6',6, TRUE),
    ('55555555-0000-0000-0000-000000000006', '11111111-0000-0000-0000-000000000001', 'Examen Preventiv',        NULL,   24, TRUE)
ON CONFLICT (id) DO NOTHING;

-- 7. Default clinic settings
INSERT INTO clinic_settings (tenant_id, working_hours, booking_slot_minutes) VALUES
    ('11111111-0000-0000-0000-000000000001',
     '{"mon":{"open":"08:00","close":"18:00"},"tue":{"open":"08:00","close":"18:00"},"wed":{"open":"08:00","close":"18:00"},"thu":{"open":"08:00","close":"18:00"},"fri":{"open":"08:00","close":"16:00"},"sat":{"open":"09:00","close":"13:00"},"sun":null}',
     15)
ON CONFLICT (tenant_id) DO NOTHING;

-- 8. Explicitly update MRN sequence just in case
UPDATE tenants SET mrn_sequence = 4825 WHERE id = '11111111-0000-0000-0000-000000000001';
