-- Seed required appointment types if truncated
INSERT INTO appointment_types (id, tenant_id, name, color_hex, duration_minutes, description) VALUES
    ('a1111111-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', 'Consultație inițială', '#0284c7', 30, 'Consult complet oftalmologic pentru pacienți noi'),
    ('a1111111-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000001', 'Control de rutină', '#10b981', 15, 'Verificare periodică și reevaluare')
ON CONFLICT (id) DO NOTHING;

-- Seed required service catalog items if truncated
INSERT INTO service_catalog (id, tenant_id, name, category, unit_price, vat_rate, currency, is_active, version) VALUES
    ('819a760d-9a50-45ca-8ad6-edcd3cb60a45', '11111111-0000-0000-0000-000000000001', 'Consultație inițială oftalmolog', 'medical', 200.0000, 19.00, 'RON', TRUE, 0)
ON CONFLICT (id) DO NOTHING;


-- 1. Patient: Adrian Diaconescu (MRN OC-004825)
INSERT INTO patients (
    id, tenant_id, mrn, first_name, last_name, date_of_birth, gender, cnp, phone, email, address, city, county, blood_type, has_portal_access, is_active, version
) VALUES (
    '44444444-0000-0000-0000-000000004825',
    '11111111-0000-0000-0000-000000000001',
    'OC-004825',
    'Adrian',
    'Diaconescu',
    '1990-05-15',
    'MALE',
    '1900515123456',
    '+40722123456',
    'adrian.diaconescu@ophthacloud.com',
    'Bulevardul Regina Elisabeta nr. 4-12',
    'București',
    'București',
    'A+',
    TRUE,
    TRUE,
    0
)
ON CONFLICT (tenant_id, mrn) DO NOTHING;

-- 2. Patient Medical History
INSERT INTO patient_medical_history (
    id, tenant_id, patient_id, has_diabetes, has_hypertension, known_allergies, current_medications, version
) VALUES (
    '44444444-1111-0000-0000-000000004825',
    '11111111-0000-0000-0000-000000000001',
    '44444444-0000-0000-0000-000000004825',
    TRUE,
    FALSE,
    'Polen, Penicilină',
    'Paracetamol la nevoie',
    0
)
ON CONFLICT (patient_id) DO NOTHING;

-- 3. Today's Appointments (CONFIRMED and CHECKED_IN)
INSERT INTO appointments (
    id, tenant_id, patient_id, appointment_type_id, doctor_id, doctor_name, start_at, end_at, duration_minutes, status, channel, room, booked_via, confirmed_at, checked_in_at, version
) VALUES (
    '55555555-1111-0000-0000-000000000001',
    '11111111-0000-0000-0000-000000000001',
    '44444444-0000-0000-0000-000000004825',
    'a1111111-0000-0000-0000-000000000001',
    '77a158c9-08d2-4ee7-b7b4-99f7d348cc88',
    'Ioana Dobre',
    CURRENT_DATE + TIME '10:00:00',
    CURRENT_DATE + TIME '10:30:00',
    30,
    'CONFIRMED',
    'IN_PERSON',
    'Cabinet 1',
    'STAFF',
    NOW(),
    NULL,
    0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO appointments (
    id, tenant_id, patient_id, appointment_type_id, doctor_id, doctor_name, start_at, end_at, duration_minutes, status, channel, room, booked_via, confirmed_at, checked_in_at, version
) VALUES (
    '55555555-2222-0000-0000-000000000002',
    '11111111-0000-0000-0000-000000000001',
    '44444444-0000-0000-0000-000000004825',
    'a1111111-0000-0000-0000-000000000002',
    '77a158c9-08d2-4ee7-b7b4-99f7d348cc88',
    'Ioana Dobre',
    CURRENT_DATE + TIME '11:00:00',
    CURRENT_DATE + TIME '11:15:00',
    15,
    'CHECKED_IN',
    'IN_PERSON',
    'Cabinet 1',
    'STAFF',
    NOW(),
    NOW(),
    0
)
ON CONFLICT (id) DO NOTHING;

-- 4. Invoices (one ISSUED for outstanding balance, one PAID for revenue metrics)
INSERT INTO invoices (
    id, tenant_id, invoice_number, patient_id, status, issued_at, due_at, paid_at, subtotal, vat_total, discount_total, total, amount_paid, currency, payment_method, version
) VALUES (
    '77777777-1111-0000-0000-000000000001',
    '11111111-0000-0000-0000-000000000001',
    'FC-2026-000005',
    '44444444-0000-0000-0000-000000004825',
    'ISSUED',
    NOW(),
    NOW() + INTERVAL '14 days',
    NULL,
    168.0700,
    31.9300,
    0.0000,
    200.0000,
    0.0000,
    'RON',
    NULL,
    0
)
ON CONFLICT (tenant_id, invoice_number) DO NOTHING;

INSERT INTO invoices (
    id, tenant_id, invoice_number, patient_id, status, issued_at, due_at, paid_at, subtotal, vat_total, discount_total, total, amount_paid, currency, payment_method, version
) VALUES (
    '77777777-2222-0000-0000-000000000002',
    '11111111-0000-0000-0000-000000000001',
    'FC-2026-000006',
    '44444444-0000-0000-0000-000000004825',
    'PAID',
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '13 days',
    NOW() - INTERVAL '1 day',
    840.3400,
    159.6600,
    0.0000,
    1000.0000,
    1000.0000,
    'RON',
    'CARD',
    0
)
ON CONFLICT (tenant_id, invoice_number) DO NOTHING;

-- 5. Invoice Lines
INSERT INTO invoice_lines (
    id, tenant_id, invoice_id, description, quantity, unit_price, vat_rate, discount_percent, line_total, service_item_id, version
) VALUES (
    '88888888-1111-0000-0000-000000000001',
    '11111111-0000-0000-0000-000000000001',
    '77777777-1111-0000-0000-000000000001',
    'Consultație inițială oftalmolog',
    1,
    168.0700,
    19.00,
    0.00,
    200.0000,
    '819a760d-9a50-45ca-8ad6-edcd3cb60a45',
    0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO invoice_lines (
    id, tenant_id, invoice_id, description, quantity, unit_price, vat_rate, discount_percent, line_total, service_item_id, version
) VALUES (
    '88888888-2222-0000-0000-000000000002',
    '11111111-0000-0000-0000-000000000001',
    '77777777-2222-0000-0000-000000000002',
    'Ramă ochelari Hugo Boss & Lentile',
    1,
    840.3400,
    19.00,
    0.00,
    1000.0000,
    NULL,
    0
)
ON CONFLICT (id) DO NOTHING;

-- 6. Optical Orders (active order in stage SENT_TO_LAB)
INSERT INTO optical_orders (
    id, tenant_id, order_number, patient_id, order_type, stage, assigned_to_id, assigned_to_name, lab_name, lab_reference, sent_to_lab_at, total_amount, deposit_paid, currency, notes, created_by_id, version
) VALUES (
    '33333333-1111-0000-0000-000000000001',
    '11111111-0000-0000-0000-000000000001',
    'CMD-2026-000005',
    '44444444-0000-0000-0000-000000004825',
    'GLASSES',
    'SENT_TO_LAB',
    '60e3448f-7f02-4522-b497-1df5d3f5e374',
    'Marcel Dima',
    'Interoptik Lab',
    'LAB-REF-999',
    NOW(),
    1200.0000,
    400.0000,
    'RON',
    'Lentile progresive cu filtru de lumină albastră.',
    'bc3f22c3-c9f8-445f-b437-93b4c7c4c049',
    0
)
ON CONFLICT (tenant_id, order_number) DO NOTHING;

-- 8. Low Stock Inventory Item (stock level 3, minimum stock 3/5 threshold)
INSERT INTO stock_items (
    id, tenant_id, service_item_id, name, category, sku, barcode, brand, current_stock, minimum_stock, unit_cost, unit_price, currency, location, is_active, version
) VALUES (
    '99999999-0000-0000-0000-000000000003',
    '11111111-0000-0000-0000-000000000001',
    NULL,
    'Ramă Ochelari Hugo Boss',
    'rame',
    'HB-FRAME-099',
    '5901234567890',
    'Hugo Boss',
    3,
    3,
    250.0000,
    450.0000,
    'RON',
    'Raft A1',
    TRUE,
    0
)
ON CONFLICT (id) DO NOTHING;

-- 7. Optical Order Items
INSERT INTO optical_order_items (
    id, tenant_id, order_id, stock_item_id, description, quantity, unit_price, discount_percent, line_total, version
) VALUES (
    '33333333-2222-0000-0000-000000000002',
    '11111111-0000-0000-0000-000000000001',
    '33333333-1111-0000-0000-000000000001',
    '99999999-0000-0000-0000-000000000003',
    'Ramă Aviator Classic + Lentile Progresive',
    1,
    1200.0000,
    0.00,
    1200.0000,
    0
)
ON CONFLICT (id) DO NOTHING;
