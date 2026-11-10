-- Optional cinema snapshots already mapped by Order; preserve historical rows.
ALTER TABLE food_orders
    ADD COLUMN IF NOT EXISTS cinema_id BIGINT,
    ADD COLUMN IF NOT EXISTS cinema_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cinema_address VARCHAR(500);
