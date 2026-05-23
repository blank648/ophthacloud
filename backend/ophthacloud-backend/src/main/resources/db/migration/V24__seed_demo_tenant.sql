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
    ('22222222-0000-0000-0000-000000000001',
     '11111111-0000-0000-0000-000000000001',
     'kc-doctor-ionescu',
     'Alexandru', 'Ionescu',
     'dr.ionescu@clinica-demo.ro',
     'DOCTOR',
     TRUE),
    ('22222222-0000-0000-0000-000000000002',
     '11111111-0000-0000-0000-000000000001',
     'kc-receptionist-maria',
     'Maria', 'Popescu',
     'maria.popescu@clinica-demo.ro',
     'RECEPTIONIST',
     TRUE)
ON CONFLICT (id) DO NOTHING;

-- 3. Demo Patients (matching prototype UI)
INSERT INTO patients (id, tenant_id, mrn, first_name, last_name, date_of_birth, gender, phone, email, is_active, has_portal_access) VALUES
    ('33333333-0000-0000-0000-000000000001',
     '11111111-0000-0000-0000-000000000001',
     'OC-004821', 'Gheorghe', 'Ionescu',
     '1963-08-15', 'MALE', '0722 111 222', 'g.ionescu@email.ro',
     TRUE, FALSE),

    ('33333333-0000-0000-0000-000000000002',
     '11111111-0000-0000-0000-000000000001',
     'OC-004822', 'Maria', 'Constantin',
     '1978-03-22', 'FEMALE', '0733 222 333', 'maria.c@email.ro',
     TRUE, FALSE),

    ('33333333-0000-0000-0000-000000000003',
     '11111111-0000-0000-0000-000000000001',
     'OC-004823', 'Alexandru', 'Dima',
     '1990-11-05', 'MALE', '0744 333 444', 'alex.dima@email.ro',
     TRUE, FALSE),

    ('33333333-0000-0000-0000-000000000004',
     '11111111-0000-0000-0000-000000000001',
     'OC-004824', 'Elena', 'Voicu',
     '1955-06-18', 'FEMALE', '0755 444 555', 'elena.v@email.ro',
     TRUE, FALSE),

    ('33333333-0000-0000-0000-000000000005',
     '11111111-0000-0000-0000-000000000001',
     'OC-004825', 'Radu', 'Mihai',
     '1985-01-30', 'MALE', '0766 555 666', 'radu.m@email.ro',
     TRUE, FALSE)
ON CONFLICT (id) DO NOTHING;

-- 4. Medical history for demo patients
INSERT INTO patient_medical_history (patient_id, tenant_id, has_glaucoma_history, active_diagnoses) VALUES
    ('33333333-0000-0000-0000-000000000001',
     '11111111-0000-0000-0000-000000000001',
     TRUE,
     '[{"icd10_code":"H40.1","icd10_name":"Glaucom cu unghi deschis","laterality":"BILATERAL"}]')
ON CONFLICT (patient_id) DO NOTHING;

INSERT INTO patient_medical_history (patient_id, tenant_id, has_diabetes, active_diagnoses) VALUES
    ('33333333-0000-0000-0000-000000000004',
     '11111111-0000-0000-0000-000000000001',
     TRUE,
     '[{"icd10_code":"H36.0","icd10_name":"Retinopatie diabetică","laterality":"BILATERAL"}]')
ON CONFLICT (patient_id) DO NOTHING;

-- 5. Demo appointments for today
INSERT INTO appointments
    (id, tenant_id, patient_id, doctor_id, doctor_name, start_at, end_at, status, chief_complaint, duration_minutes)
VALUES
    ('44444444-0000-0000-0000-000000000001',
     '11111111-0000-0000-0000-000000000001',
     '33333333-0000-0000-0000-000000000001',
     '22222222-0000-0000-0000-000000000001',
     'Dr. Alexandru Ionescu',
     CURRENT_DATE + INTERVAL '9 hours',
     CURRENT_DATE + INTERVAL '9 hours 30 minutes',
     'CHECKED_IN',
     'Control glaucom — vedere încetoșată matinal',
     30),

    ('44444444-0000-0000-0000-000000000002',
     '11111111-0000-0000-0000-000000000001',
     '33333333-0000-0000-0000-000000000002',
     '22222222-0000-0000-0000-000000000001',
     'Dr. Alexandru Ionescu',
     CURRENT_DATE + INTERVAL '10 hours',
     CURRENT_DATE + INTERVAL '10 hours 30 minutes',
     'CONFIRMED',
     'Consultație inițială — prescripție ochelari',
     30),

    ('44444444-0000-0000-0000-000000000003',
     '11111111-0000-0000-0000-000000000001',
     '33333333-0000-0000-0000-000000000003',
     '22222222-0000-0000-0000-000000000001',
     'Dr. Alexandru Ionescu',
     CURRENT_DATE + INTERVAL '11 hours',
     CURRENT_DATE + INTERVAL '11 hours 30 minutes',
     'BOOKED',
     'Miopie progresivă — control anual',
     30),

    ('44444444-0000-0000-0000-000000000004',
     '11111111-0000-0000-0000-000000000001',
     '33333333-0000-0000-0000-000000000004',
     '22222222-0000-0000-0000-000000000001',
     'Dr. Alexandru Ionescu',
     CURRENT_DATE + INTERVAL '14 hours',
     CURRENT_DATE + INTERVAL '14 hours 30 minutes',
     'BOOKED',
     'Control retinopatie diabetică',
     30),

    ('44444444-0000-0000-0000-000000000005',
     '11111111-0000-0000-0000-000000000001',
     '33333333-0000-0000-0000-000000000005',
     '22222222-0000-0000-0000-000000000001',
     'Dr. Alexandru Ionescu',
     CURRENT_DATE + INTERVAL '15 hours',
     CURRENT_DATE + INTERVAL '15 hours 30 minutes',
     'BOOKED',
     'Astigmatism — prescripție lentile de contact',
     30)
ON CONFLICT (id) DO NOTHING;

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
