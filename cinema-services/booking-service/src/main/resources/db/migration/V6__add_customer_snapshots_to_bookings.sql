-- Historical bookings may not have customer snapshots; preserve nullable mappings.
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS customer_name_snapshot VARCHAR(255),
    ADD COLUMN IF NOT EXISTS customer_phone_snapshot VARCHAR(50),
    ADD COLUMN IF NOT EXISTS customer_email_snapshot VARCHAR(255);
