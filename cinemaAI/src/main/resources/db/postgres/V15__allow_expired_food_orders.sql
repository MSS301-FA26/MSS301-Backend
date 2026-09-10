-- Extend the existing food-order state constraint for timed-out checkout sessions.

ALTER TABLE food_orders DROP CONSTRAINT IF EXISTS food_orders_status_check;
ALTER TABLE food_orders ADD CONSTRAINT food_orders_status_check
    CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'EXPIRED'));
