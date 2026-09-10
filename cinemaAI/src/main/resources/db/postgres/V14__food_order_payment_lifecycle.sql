-- Keep standalone concession checkout recoverable for a bounded payment window.

ALTER TABLE food_orders ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;
ALTER TABLE food_orders ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;

UPDATE food_orders
SET expires_at = created_at + INTERVAL '15 minutes'
WHERE status = 'PENDING_PAYMENT' AND expires_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_food_orders_pending_expiry
    ON food_orders (status, expires_at);
