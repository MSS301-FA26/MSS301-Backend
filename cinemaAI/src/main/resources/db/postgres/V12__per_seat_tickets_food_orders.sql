-- Per-seat tickets + food orders (schema is applied by JPA ddl-auto=update in dev;
-- this script documents the change for environments that apply SQL manually, like V11)

ALTER TABLE booking_seats ADD COLUMN IF NOT EXISTS ticket_code VARCHAR(60);
ALTER TABLE booking_seats ADD COLUMN IF NOT EXISTS qr_code VARCHAR(500);
ALTER TABLE booking_seats ADD COLUMN IF NOT EXISTS ticket_type VARCHAR(30);
ALTER TABLE booking_seats ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS ux_booking_seats_ticket_code
    ON booking_seats (ticket_code) WHERE ticket_code IS NOT NULL;

CREATE TABLE IF NOT EXISTS food_orders (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings (id),
    order_code VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    paid_at TIMESTAMP,
    created_by_staff BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_food_orders_booking ON food_orders (booking_id);

ALTER TABLE booking_food_items ADD COLUMN IF NOT EXISTS food_order_id BIGINT REFERENCES food_orders (id);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS food_order_id BIGINT REFERENCES food_orders (id);
