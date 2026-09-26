ALTER TABLE rooms ADD COLUMN IF NOT EXISTS aisle_position INTEGER DEFAULT 0 NOT NULL;
UPDATE rooms SET aisle_position = 0 WHERE aisle_position IS NULL;
