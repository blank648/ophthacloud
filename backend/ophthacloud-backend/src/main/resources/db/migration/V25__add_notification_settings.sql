-- V25__add_notification_settings.sql
ALTER TABLE clinic_settings
    ADD COLUMN quiet_hours_start TIME DEFAULT '20:00:00',
    ADD COLUMN quiet_hours_end TIME DEFAULT '08:00:00',
    ADD COLUMN max_sms_per_patient INT DEFAULT 2;
