-- Update notification rules to use dynamic {{clinicName}} template placeholder instead of hardcoded VisionMed
UPDATE notification_rules
SET config_data = '{"trigger_type": "APPOINTMENT_CONFIRMED", "channels": ["EMAIL", "SMS"], "template_email_subject": "Confirmare programare {{clinicName}}", "template_email_body": "Bună {{patientFirstName}}, programarea ta la {{clinicName}} din data de {{appointmentDate}} la ora {{appointmentTime}} a fost confirmată. Te așteptăm!"}'::jsonb
WHERE id = '88888888-0000-0000-0000-000000000001';

UPDATE notification_rules
SET config_data = '{"trigger_type": "PRESCRIPTION_SIGNED", "channels": ["EMAIL"], "template_email_subject": "Rețetă medicală digitală {{clinicName}}", "template_email_body": "Bună {{patientFirstName}}, rețeta ta eliberată astăzi a fost semnată digital de medic. O poți descărca din portalul tău de pacient."}'::jsonb
WHERE id = '88888888-0000-0000-0000-000000000004';

UPDATE notification_rules
SET config_data = '{"trigger_type": "CONSULTATION_SIGNED", "channels": ["EMAIL"], "template_email_subject": "Raport consult medical {{clinicName}}", "template_email_body": "Bună {{patientFirstName}}, fișa medicală a consultației tale de astăzi, semnată de Dr. {{doctorName}}, este disponibilă în format digital."}'::jsonb
WHERE id = '88888888-0000-0000-0000-000000000005';
