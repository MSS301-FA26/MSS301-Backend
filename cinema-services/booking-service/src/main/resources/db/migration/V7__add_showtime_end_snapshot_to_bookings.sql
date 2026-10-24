-- Align the optional showtime end snapshot with Booking's existing mapping.
ALTER TABLE bookings ADD COLUMN showtime_end_snapshot TIMESTAMP;
