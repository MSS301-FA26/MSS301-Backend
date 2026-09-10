-- Allow customers to buy concessions without a movie booking.
-- Existing add-on orders are backfilled from their booking owner.

ALTER TABLE food_orders ADD COLUMN IF NOT EXISTS customer_id BIGINT REFERENCES users (id);

UPDATE food_orders fo
SET customer_id = b.user_id
FROM bookings b
WHERE fo.booking_id = b.id AND fo.customer_id IS NULL;

ALTER TABLE food_orders ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE food_orders ALTER COLUMN booking_id DROP NOT NULL;
ALTER TABLE booking_food_items ALTER COLUMN booking_id DROP NOT NULL;
ALTER TABLE payments ALTER COLUMN booking_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_food_orders_customer ON food_orders (customer_id);
