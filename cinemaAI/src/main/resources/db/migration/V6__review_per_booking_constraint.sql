ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_user_id_movie_id_key;
ALTER TABLE reviews DROP CONSTRAINT IF EXISTS uk_reviews_user_movie;

DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    FOR constraint_record IN
        SELECT n.nspname, c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE t.relname = 'reviews'
          AND c.contype = 'u'
          AND (
              SELECT array_agg(a.attname::text ORDER BY a.attname::text)
              FROM unnest(c.conkey) key(attnum)
              JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = key.attnum
          ) = ARRAY['movie_id', 'user_id']
    LOOP
        EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT %I', constraint_record.nspname, 'reviews', constraint_record.conname);
    END LOOP;
END $$;

DO $$
DECLARE
    index_record RECORD;
BEGIN
    FOR index_record IN
        SELECT schemaname, indexname
        FROM pg_indexes
        WHERE tablename = 'reviews'
          AND indexdef ILIKE '%unique%'
          AND indexdef ILIKE '%user_id%'
          AND indexdef ILIKE '%movie_id%'
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS %I.%I', index_record.schemaname, index_record.indexname);
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_reviews_booking_active
ON reviews(booking_id)
WHERE booking_id IS NOT NULL AND status <> 'DELETED';
