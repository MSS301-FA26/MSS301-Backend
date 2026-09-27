-- V11: Standardize seat types (SINGLE, COUPLE) and pricing rules
ALTER TABLE seats DROP CONSTRAINT IF EXISTS seats_seat_type_check;
ALTER TABLE seats ADD CONSTRAINT seats_seat_type_check CHECK (seat_type IN ('SINGLE', 'COUPLE', 'NORMAL', 'STANDARD', 'VIP'));

UPDATE seats SET seat_type = 'SINGLE' WHERE seat_type IN ('NORMAL', 'STANDARD');
UPDATE seats SET seat_type = 'COUPLE' WHERE row_label = 'E' OR seat_type = 'VIP';

ALTER TABLE booking_seats DROP CONSTRAINT IF EXISTS booking_seats_ticket_type_check;
ALTER TABLE booking_seats ADD CONSTRAINT booking_seats_ticket_type_check CHECK (ticket_type IS NULL OR ticket_type IN ('SINGLE', 'COUPLE', 'ADULT', 'CHILD', 'STUDENT'));

ALTER TABLE ticket_pricing_rules ADD COLUMN IF NOT EXISTS cinema_id bigint REFERENCES cinemas(id);
ALTER TABLE ticket_pricing_rules ADD COLUMN IF NOT EXISTS effective_from timestamp(6) without time zone;
ALTER TABLE ticket_pricing_rules ADD COLUMN IF NOT EXISTS effective_to timestamp(6) without time zone;
ALTER TABLE ticket_pricing_rules ALTER COLUMN ticket_type DROP NOT NULL;
ALTER TABLE ticket_pricing_rules ALTER COLUMN room_type DROP NOT NULL;

ALTER TABLE ticket_pricing_rules DROP CONSTRAINT IF EXISTS ticket_pricing_rules_seat_type_check;
ALTER TABLE ticket_pricing_rules ADD CONSTRAINT ticket_pricing_rules_seat_type_check CHECK (seat_type IS NULL OR seat_type IN ('SINGLE', 'COUPLE', 'NORMAL', 'STANDARD', 'VIP'));

ALTER TABLE ticket_pricing_rules DROP CONSTRAINT IF EXISTS ticket_pricing_rules_ticket_type_check;
ALTER TABLE ticket_pricing_rules ADD CONSTRAINT ticket_pricing_rules_ticket_type_check CHECK (ticket_type IS NULL OR ticket_type IN ('ADULT', 'CHILD', 'STUDENT', 'SINGLE', 'COUPLE'));

-- Default active pricing rules if none exist
INSERT INTO ticket_pricing_rules (created_at, updated_at, active, holiday, price, room_type, seat_type, ticket_type, weekend, cinema_id)
SELECT NOW(), NOW(), true, false, 90000.00, 'STANDARD', 'SINGLE', NULL, false, 1
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE seat_type = 'SINGLE');

INSERT INTO ticket_pricing_rules (created_at, updated_at, active, holiday, price, room_type, seat_type, ticket_type, weekend, cinema_id)
SELECT NOW(), NOW(), true, false, 180000.00, 'STANDARD', 'COUPLE', NULL, false, 1
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE seat_type = 'COUPLE');
