-- Optional cinema snapshots already mapped by Order; preserve historical rows.
ALTER TABLE food_orders
    ADD COLUMN cinema_id BIGINT,
    ADD COLUMN cinema_name VARCHAR(255),
    ADD COLUMN cinema_address VARCHAR(500);
