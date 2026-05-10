-- V20__add_notification_retry.sql
-- Add retry tracking fields to notification_log to support exponential backoff.

ALTER TABLE notification_log
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at TIMESTAMPTZ;
