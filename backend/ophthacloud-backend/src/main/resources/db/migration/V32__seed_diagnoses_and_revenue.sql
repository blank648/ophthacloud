-- Ensure tenant, patient, patient medical history and prerequisite invoice exist (in case DB was truncated in tests)
INSERT INTO tenants (id, slug, name, keycloak_realm, is_active, mrn_sequence) VALUES
    ('11111111-0000-0000-0000-000000000001', 'clinica-demo', 'Clinica Oftalmologică Demo SRL', 'ophthacloud-demo', TRUE, 4825)
ON CONFLICT (id) DO NOTHING;

INSERT INTO patients (
    id, tenant_id, mrn, first_name, last_name, date_of_birth, gender, cnp, phone, email, address, city, county, blood_type, has_portal_access, is_active, version
) VALUES (
    '44444444-0000-0000-0000-000000004825', '11111111-0000-0000-0000-000000000001', 'OC-004825', 'Adrian', 'Diaconescu', '1990-05-15', 'MALE', '1900515123456', '+40722123456', 'adrian.diaconescu@ophthacloud.com', 'Bulevardul Regina Elisabeta nr. 4-12', 'București', 'București', 'A+', TRUE, TRUE, 0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO patient_medical_history (
    id, tenant_id, patient_id, has_diabetes, has_hypertension, known_allergies, current_medications, version
) VALUES (
    '44444444-1111-0000-0000-000000004825', '11111111-0000-0000-0000-000000000001', '44444444-0000-0000-0000-000000004825', TRUE, FALSE, 'Polen, Penicilină', 'Paracetamol la nevoie', 0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO invoices (
    id, tenant_id, invoice_number, patient_id, status, issued_at, due_at, paid_at, subtotal, vat_total, discount_total, total, amount_paid, currency, payment_method, version
) VALUES (
    '77777777-2222-0000-0000-000000000002', '11111111-0000-0000-0000-000000000001', 'FC-2026-000006', '44444444-0000-0000-0000-000000004825', 'PAID', NOW() - INTERVAL '1 day', NOW() + INTERVAL '13 days', NOW() - INTERVAL '1 day', 840.3400, 159.6600, 0.0000, 1000.0000, 1000.0000, 'RON', 'CARD', 0
)
ON CONFLICT (id) DO NOTHING;

-- Fix any mismatch in existing staff member emails to prevent unique constraint violation
UPDATE staff_members SET email = 'idobre@visionmed.com' WHERE id = '77a158c9-08d2-4ee7-b7b4-99f7d348cc88';

-- Seed staff members for demo doctors
INSERT INTO staff_members (id, tenant_id, keycloak_user_id, first_name, last_name, email, role, is_active) VALUES
    ('77a158c9-08d2-4ee7-b7b4-99f7d348cc88',
     '11111111-0000-0000-0000-000000000001',
     'ef416065-ebd1-4317-bbfd-349586c665e4',
     'Ioana', 'Dobre',
     'idobre@visionmed.com',
     'DOCTOR',
     TRUE),
    ('22222222-1111-1111-1111-222222222222',
     '11111111-0000-0000-0000-000000000001',
     'ef416065-ebd1-4317-bbfd-349586c665e5',
     'Vasile', 'Dobrescu',
     'idobrescu@visionmed.com',
     'DOCTOR',
     TRUE)
ON CONFLICT (id) DO NOTHING;

-- Create dynamic consultations for Adrian Diaconescu to populate Top 5 Diagnoses and Revenue
INSERT INTO consultations (id, tenant_id, patient_id, appointment_id, doctor_id, doctor_name, status, consultation_date, chief_complaint, sections_completed, signed_at, signed_by_id, version) VALUES
    ('11111111-2222-3333-4444-555555555551', '11111111-0000-0000-0000-000000000001', '44444444-0000-0000-0000-000000004825', NULL, '77a158c9-08d2-4ee7-b7b4-99f7d348cc88', 'Dr. Ioana Dobre', 'SIGNED', CURRENT_DATE - 1, 'Scăderea acuității vizuale', 511, NOW() - INTERVAL '1 day', '77a158c9-08d2-4ee7-b7b4-99f7d348cc88', 1),
    ('11111111-2222-3333-4444-555555555552', '11111111-0000-0000-0000-000000000001', '44444444-0000-0000-0000-000000004825', NULL, '22222222-1111-1111-1111-222222222222', 'Dr. Vasile Dobrescu', 'SIGNED', CURRENT_DATE - 2, 'Control periodic glaucom', 511, NOW() - INTERVAL '2 days', '22222222-1111-1111-1111-222222222222', 1)
ON CONFLICT (id) DO NOTHING;

-- Seed section F for these consultations in consultation_sections
INSERT INTO consultation_sections (id, tenant_id, consultation_id, section_code, section_data, is_completed, completed_at, version) VALUES
    (gen_random_uuid(), '11111111-0000-0000-0000-000000000001', '11111111-2222-3333-4444-555555555551', 'F', '{"diagnoses": [{"code": "H35.31", "name": "AMD uscat", "primary": true}]}', TRUE, NOW() - INTERVAL '1 day', 1),
    (gen_random_uuid(), '11111111-0000-0000-0000-000000000001', '11111111-2222-3333-4444-555555555552', 'F', '{"diagnoses": [{"code": "H40.1", "name": "Glaucom unghi deschis", "primary": true}]}', TRUE, NOW() - INTERVAL '2 days', 1)
ON CONFLICT (consultation_id, section_code) DO NOTHING;

-- Seed active diagnoses in patient medical history
UPDATE patient_medical_history
SET active_diagnoses = '[{"icd10Code": "H35.31", "icd10Name": "AMD uscat", "laterality": "OU", "sinceDate": "2026-05-20"}, {"icd10Code": "H40.1", "icd10Name": "Glaucom unghi deschis", "laterality": "OU", "sinceDate": "2026-05-21"}]'::jsonb
WHERE patient_id = '44444444-0000-0000-0000-000000004825';

-- Link first invoice to first consultation
UPDATE invoices
SET consultation_id = '11111111-2222-3333-4444-555555555551'
WHERE id = '77777777-2222-0000-0000-000000000002';

-- Add another paid invoice linked to the second consultation
INSERT INTO invoices (
    id, tenant_id, invoice_number, patient_id, status, issued_at, due_at, paid_at, subtotal, vat_total, discount_total, total, amount_paid, currency, version, consultation_id
) VALUES (
    '77777777-2222-0000-0000-000000000003',
    '11111111-0000-0000-0000-000000000001',
    'FC-2026-000007',
    '44444444-0000-0000-0000-000000004825',
    'PAID',
    NOW() - INTERVAL '2 days',
    NOW() + INTERVAL '12 days',
    NOW() - INTERVAL '2 days',
    672.2700,
    127.7300,
    0.0000,
    800.0000,
    800.0000,
    'RON',
    0,
    '11111111-2222-3333-4444-555555555552'
)
ON CONFLICT (tenant_id, invoice_number) DO NOTHING;
