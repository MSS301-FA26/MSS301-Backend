IF COL_LENGTH('dbo.showtimes', 'cancellation_reason') IS NULL
BEGIN
    ALTER TABLE dbo.showtimes ADD cancellation_reason NVARCHAR(MAX);
END

IF COL_LENGTH('dbo.showtimes', 'cancelled_at') IS NULL
BEGIN
    ALTER TABLE dbo.showtimes ADD cancelled_at DATETIME2;
END

IF COL_LENGTH('dbo.bookings', 'refund_method') IS NULL
BEGIN
    ALTER TABLE dbo.bookings ADD refund_method NVARCHAR(50);
END

IF COL_LENGTH('dbo.bookings', 'bulk_refund') IS NULL
BEGIN
    ALTER TABLE dbo.bookings ADD bulk_refund BIT NOT NULL CONSTRAINT df_bookings_bulk_refund DEFAULT 0;
END

IF COL_LENGTH('dbo.bookings', 'refund_retry_attempts') IS NULL
BEGIN
    ALTER TABLE dbo.bookings ADD refund_retry_attempts INT NOT NULL CONSTRAINT df_bookings_refund_retry_attempts DEFAULT 0;
END

IF COL_LENGTH('dbo.bookings', 'last_refund_attempt_at') IS NULL
BEGIN
    ALTER TABLE dbo.bookings ADD last_refund_attempt_at DATETIME2;
END
