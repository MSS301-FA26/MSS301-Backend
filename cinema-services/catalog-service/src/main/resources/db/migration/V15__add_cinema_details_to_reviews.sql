-- Migration V15: Add cinema name and address to reviews
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS cinema_name VARCHAR(255);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS cinema_address VARCHAR(500);

-- Backfill existing reviews with first active cinema if any
UPDATE reviews r
SET 
    cinema_name = COALESCE(r.cinema_name, c.name),
    cinema_address = COALESCE(r.cinema_address, c.address || CASE WHEN c.city IS NOT NULL AND c.city <> '' THEN ', ' || c.city ELSE '' END)
FROM (SELECT name, address, city FROM cinemas ORDER BY id ASC LIMIT 1) c
WHERE r.cinema_name IS NULL;
