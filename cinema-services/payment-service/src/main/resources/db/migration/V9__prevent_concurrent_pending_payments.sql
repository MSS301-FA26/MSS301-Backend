-- Allow historical FAILED attempts, but at most one active PENDING attempt.
-- Existing duplicates are reported and never deleted automatically.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM payments WHERE booking_id IS NOT NULL AND status = 'PENDING' GROUP BY booking_id HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Duplicate PENDING booking payments exist';
    END IF;
    IF EXISTS (SELECT 1 FROM payments WHERE food_order_id IS NOT NULL AND status = 'PENDING' GROUP BY food_order_id HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Duplicate PENDING food-order payments exist';
    END IF;
END $$;

CREATE UNIQUE INDEX ux_payments_pending_booking ON payments (booking_id)
    WHERE booking_id IS NOT NULL AND status = 'PENDING';
CREATE UNIQUE INDEX ux_payments_pending_food_order ON payments (food_order_id)
    WHERE food_order_id IS NOT NULL AND status = 'PENDING';
