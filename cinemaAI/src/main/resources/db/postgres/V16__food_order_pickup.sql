ALTER TABLE food_orders
    ADD COLUMN IF NOT EXISTS picked_up_at TIMESTAMP;

ALTER TABLE food_orders
    DROP CONSTRAINT IF EXISTS food_orders_status_check;

ALTER TABLE food_orders
    ADD CONSTRAINT food_orders_status_check
        CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PICKED_UP', 'CANCELLED', 'EXPIRED'));
