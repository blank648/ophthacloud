-- Clear historic pre-seeded notification logs to keep the audit history authentic and user-driven
DELETE FROM notification_log WHERE id IN (
    '99999999-0000-0000-0000-000000000001',
    '99999999-0000-0000-0000-000000000002',
    '99999999-0000-0000-0000-000000000003',
    '99999999-0000-0000-0000-000000000004',
    '99999999-0000-0000-0000-000000000005'
);
