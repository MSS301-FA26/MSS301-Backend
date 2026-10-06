-- A payment can emit each domain event once, even when its callback is retried.
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS deduplication_key VARCHAR(150);

UPDATE outbox_events
SET deduplication_key = event_type || ':' || aggregate_id || ':' || id
WHERE deduplication_key IS NULL;

ALTER TABLE outbox_events
    ALTER COLUMN deduplication_key SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_outbox_events_deduplication_key
    ON outbox_events (deduplication_key);

