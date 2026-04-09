-- Add status column required by Spring Modulith 2.0.0
ALTER TABLE event_publication
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED';

-- Backfill: records with a completion_date are already COMPLETED
UPDATE event_publication
SET status = 'COMPLETED'
WHERE completion_date IS NOT NULL;