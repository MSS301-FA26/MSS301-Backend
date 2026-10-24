-- Historical bookings may not have customer snapshots; preserve nullable mappings.
ALTER TABLE bookings
    ADD COLUMN customer_name_snapshot VARCHAR(255),
    ADD COLUMN customer_phone_snapshot VARCHAR(50),
    ADD COLUMN customer_email_snapshot VARCHAR(255);
