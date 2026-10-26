-- Align the optional showtime end snapshot with Booking's existing mapping.
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS showtime_end_snapshot TIMESTAMP;
