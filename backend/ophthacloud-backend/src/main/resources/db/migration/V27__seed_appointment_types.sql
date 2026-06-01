-- V27__seed_appointment_types.sql
INSERT INTO appointment_types (id, tenant_id, name, color_hex, duration_minutes, description) VALUES
    ('a1111111-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', 'Consultație inițială', '#0284c7', 30, 'Consult complet oftalmologic pentru pacienți noi'),
    ('a1111111-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000001', 'Control de rutină', '#10b981', 15, 'Verificare periodică și reevaluare'),
    ('a1111111-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000001', 'Urgență', '#ef4444', 30, 'Consult pentru afecțiuni acute'),
    ('a1111111-0000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000001', 'Investigație OCT', '#8b5cf6', 20, 'Tomografie în coerență optică')
ON CONFLICT DO NOTHING;
