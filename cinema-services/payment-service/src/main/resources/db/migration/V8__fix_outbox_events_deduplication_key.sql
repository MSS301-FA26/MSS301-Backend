-- Migration V8: Fix deduplication_key column constraint in outbox_events
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'outbox_events' AND column_name = 'deduplication_key'
    ) THEN
        ALTER TABLE outbox_events ALTER COLUMN deduplication_key DROP NOT NULL;
    ELSE
        ALTER TABLE outbox_events ADD COLUMN deduplication_key VARCHAR(255);
    END IF;
END $$;