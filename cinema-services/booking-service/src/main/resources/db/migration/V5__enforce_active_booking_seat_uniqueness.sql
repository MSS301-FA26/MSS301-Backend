-- A seat may be held, sold, or checked in by only one booking for a showtime.
-- Released seats remain as history and therefore are deliberately excluded.
CREATE UNIQUE INDEX uq_booking_seats_active_showtime_seat
    ON booking_seats (showtime_id, seat_id)
    WHERE status IN ('HOLDING', 'BOOKED', 'CHECKED_IN');
