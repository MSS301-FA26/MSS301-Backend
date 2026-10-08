-- A clean database must contain the minimum catalog required by the checkout flow.
-- This replaces the former runtime-only cinema/room bootstrap.

INSERT INTO cinemas (name, address, city, phone, status, created_at, updated_at)
SELECT 'CinemaAI Central', '1 Cinema Street', 'Ho Chi Minh City', '0900000000', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM cinemas WHERE name = 'CinemaAI Central');

INSERT INTO rooms (
    cinema_id, name, room_type, row_count, column_count, status,
    standard_price, vip_price, couple_price, aisle_position, created_at, updated_at
)
SELECT c.id, 'Hall 1', 'STANDARD', 2, 4, 'ACTIVE',
       60000, 90000, 150000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cinemas c
WHERE c.name = 'CinemaAI Central'
  AND NOT EXISTS (
      SELECT 1 FROM rooms r WHERE r.cinema_id = c.id AND r.name = 'Hall 1'
  );

INSERT INTO seat_rows (room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT r.id, 'A', 1, 1, 'STANDARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM rooms r
WHERE r.name = 'Hall 1'
  AND NOT EXISTS (SELECT 1 FROM seat_rows sr WHERE sr.room_id = r.id AND sr.row_label = 'A');

INSERT INTO seat_rows (room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT r.id, 'B', 2, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM rooms r
WHERE r.name = 'Hall 1'
  AND NOT EXISTS (SELECT 1 FROM seat_rows sr WHERE sr.room_id = r.id AND sr.row_label = 'B');

INSERT INTO seats (room_id, seat_row_id, row_label, seat_number, display_column, seat_type, status, created_at, updated_at)
SELECT sr.room_id, sr.id, sr.row_label, n.seat_number, n.seat_number, sr.row_type, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seat_rows sr
JOIN rooms r ON r.id = sr.room_id
CROSS JOIN (VALUES (1), (2), (3), (4)) AS n(seat_number)
WHERE r.name = 'Hall 1'
  AND NOT EXISTS (
      SELECT 1 FROM seats s
      WHERE s.room_id = sr.room_id AND s.row_label = sr.row_label AND s.seat_number = n.seat_number
  );

INSERT INTO showtimes (
    movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
    adult_standard_price, child_standard_price, student_standard_price,
    adult_vip_price, child_vip_price, student_vip_price,
    adult_couple_price, child_couple_price, student_couple_price,
    weekend_surcharge, holiday_surcharge, late_night_surcharge_amount,
    status, created_at, updated_at
)
SELECT m.id, r.id,
       CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '10' HOUR,
       CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '12' HOUR,
       60000, 90000, 150000,
       60000, 50000, 55000,
       90000, 80000, 85000,
       150000, 140000, 145000,
       FALSE, FALSE, 20000,
       'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM movies ORDER BY id LIMIT 1) m
CROSS JOIN (SELECT id FROM rooms WHERE name = 'Hall 1' ORDER BY id LIMIT 1) r
WHERE NOT EXISTS (SELECT 1 FROM showtimes);

INSERT INTO ticket_pricing_rules (
    ticket_type, room_type, seat_type, weekend, holiday, price, active,
    cinema_id, created_at, updated_at
)
SELECT 'ADULT', 'STANDARD', 'STANDARD', FALSE, FALSE, 60000, TRUE,
       c.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cinemas c
WHERE c.name = 'CinemaAI Central'
  AND NOT EXISTS (
      SELECT 1 FROM ticket_pricing_rules t
      WHERE t.cinema_id = c.id
        AND t.ticket_type = 'ADULT'
        AND t.room_type = 'STANDARD'
        AND t.seat_type = 'STANDARD'
        AND t.weekend = FALSE
        AND t.holiday = FALSE
  );
