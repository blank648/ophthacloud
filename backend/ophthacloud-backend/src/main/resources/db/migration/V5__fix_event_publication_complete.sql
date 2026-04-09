-- Drop incomplete table created in V4 and recreate with full
-- Spring Modulith 2.0.0 schema including all required columns

DROP TABLE IF EXISTS event_publication;

CREATE TABLE event_publication (
                                   id                  UUID                        NOT NULL,
                                   listener_id         TEXT                        NOT NULL,
                                   event_type          TEXT                        NOT NULL,
                                   serialized_event    TEXT                        NOT NULL,
                                   publication_date    TIMESTAMP WITH TIME ZONE    NOT NULL,
                                   completion_date     TIMESTAMP WITH TIME ZONE,
                                   completion_attempts INTEGER                     NOT NULL DEFAULT 0,
                                   CONSTRAINT pk_event_publication PRIMARY KEY (id)
);

CREATE INDEX idx_event_publication_completion_date
    ON event_publication (completion_date);

CREATE INDEX idx_event_publication_event_type
    ON event_publication (event_type);