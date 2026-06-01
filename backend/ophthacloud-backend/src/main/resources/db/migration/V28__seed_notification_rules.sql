-- Seed notification rules and logs for demo tenant
-- Tenant ID: 11111111-0000-0000-0000-000000000001

-- 1. Default Notification Rules
INSERT INTO notification_rules (id, tenant_id, name, config_data, is_active) VALUES
    ('88888888-0000-0000-0000-000000000001', 
     '11111111-0000-0000-0000-000000000001', 
     'Confirmare Programare (Email + SMS)', 
     '{"trigger_type": "APPOINTMENT_CONFIRMED", "channels": ["EMAIL", "SMS"], "template_email_subject": "Confirmare programare clinica VisionMed", "template_email_body": "Bună {{patientFirstName}}, programarea ta la VisionMed din data de {{appointmentDate}} la ora {{appointmentTime}} a fost confirmată. Te așteptăm!"}'::jsonb, 
     TRUE),
     
    ('88888888-0000-0000-0000-000000000002', 
     '11111111-0000-0000-0000-000000000001', 
     'Alertă Stoc Critic (Admin Email)', 
     '{"trigger_type": "LOW_STOCK", "channels": ["EMAIL"], "template_email_subject": "Alertă stoc critic: {{itemName}}", "template_email_body": "Produsul {{itemName}} (SKU: {{sku}}) a scăzut sub stocul minim! Stoc curent: {{currentStock}}. Vă rugăm să reaprovizionați."}'::jsonb, 
     TRUE),
     
    ('88888888-0000-0000-0000-000000000003', 
     '11111111-0000-0000-0000-000000000001', 
     'Comandă Optică Gata (SMS)', 
     '{"trigger_type": "ORDER_READY", "channels": ["SMS"], "template_email_subject": "", "template_email_body": "Bună {{patientFirstName}}, ochelarii tăi (comanda nr. {{orderNumber}}) sunt gata! Te așteptăm în clinică pentru ridicare."}'::jsonb, 
     TRUE),
     
    ('88888888-0000-0000-0000-000000000004', 
     '11111111-0000-0000-0000-000000000001', 
     'Rețetă Medicală Digitală (Email)', 
     '{"trigger_type": "PRESCRIPTION_SIGNED", "channels": ["EMAIL"], "template_email_subject": "Rețetă medicală digitală VisionMed", "template_email_body": "Bună {{patientFirstName}}, rețeta ta eliberată astăzi a fost semnată digital de medic. O poți descărca din portalul tău de pacient."}'::jsonb, 
     TRUE),
     
    ('88888888-0000-0000-0000-000000000005', 
     '11111111-0000-0000-0000-000000000001', 
     'Fișă Consultație Finalizată (Email)', 
     '{"trigger_type": "CONSULTATION_SIGNED", "channels": ["EMAIL"], "template_email_subject": "Raport consult medical VisionMed", "template_email_body": "Bună {{patientFirstName}}, fișa medicală a consultației tale de astăzi, semnată de Dr. {{doctorName}}, este disponibilă în format digital."}'::jsonb, 
     TRUE)
ON CONFLICT (id) DO NOTHING;

-- 2. Default Sent Logs (Historic Audit)
INSERT INTO notification_log (id, tenant_id, patient_id, rule_id, channel, status, recipient_address, subject, body_preview, sent_at) VALUES
    ('99999999-0000-0000-0000-000000000001', 
     '11111111-0000-0000-0000-000000000001', 
     NULL, 
     '88888888-0000-0000-0000-000000000001', 
     'SMS', 
     'SENT', 
     '+40722123456', 
     NULL, 
     'Bună Ion, programarea ta la VisionMed din data de 31 Mai la ora 10:00 a fost confirmată. Te așteptăm!', 
     NOW() - INTERVAL '2 hours'),
     
    ('99999999-0000-0000-0000-000000000002', 
     '11111111-0000-0000-0000-000000000001', 
     NULL, 
     '88888888-0000-0000-0000-000000000001', 
     'EMAIL', 
     'SENT', 
     'ion.marinescu@gmail.com', 
     'Confirmare programare clinica VisionMed', 
     'Bună Ion, programarea ta la VisionMed din data de 31 Mai la ora 10:00 a fost confirmată. Te așteptăm!', 
     NOW() - INTERVAL '2 hours'),
     
    ('99999999-0000-0000-0000-000000000003', 
     '11111111-0000-0000-0000-000000000001', 
     NULL, 
     '88888888-0000-0000-0000-000000000002', 
     'EMAIL', 
     'SENT', 
     'manager@visionmed.ro', 
     'Alertă stoc critic: Rame Ray-Ban RB5154', 
     'Produsul Rame Ray-Ban RB5154 (SKU: RB5154-M) a scăzut sub stocul minim! Stoc curent: 2. Vă rugăm să reaprovizionați.', 
     NOW() - INTERVAL '1 day'),
     
    ('99999999-0000-0000-0000-000000000004', 
     '11111111-0000-0000-0000-000000000001', 
     NULL, 
     '88888888-0000-0000-0000-000000000003', 
     'SMS', 
     'SENT', 
     '+40733987654', 
     NULL, 
     'Bună Elena, ochelarii tăi (comanda nr. ORD-1002) sunt gata! Te așteptăm în clinică pentru ridicare.', 
     NOW() - INTERVAL '1 day'),
     
    ('99999999-0000-0000-0000-000000000005', 
     '11111111-0000-0000-0000-000000000001', 
     NULL, 
     '88888888-0000-0000-0000-000000000004', 
     'EMAIL', 
     'FAILED', 
     'andrei.popescu@yahoo.com', 
     'Rețetă medicală digitală VisionMed', 
     'Bună Andrei, rețeta ta eliberată astăzi a fost semnată digital de medic. O poți descărca din portalul tău de pacient.', 
     NOW() - INTERVAL '2 days')
ON CONFLICT (id) DO NOTHING;
