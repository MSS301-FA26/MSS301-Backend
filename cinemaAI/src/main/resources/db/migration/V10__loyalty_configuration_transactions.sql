IF COL_LENGTH('dbo.bookings', 'loyalty_points_redeemed') IS NULL
BEGIN
    ALTER TABLE dbo.bookings
        ADD loyalty_points_redeemed INT NOT NULL CONSTRAINT df_bookings_loyalty_points_redeemed DEFAULT 0;
END

IF OBJECT_ID('dbo.loyalty_configurations', 'U') IS NULL
CREATE TABLE dbo.loyalty_configurations
(
    id                    BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    earning_rate_percent  DECIMAL(5, 2) NOT NULL DEFAULT 1.00,
    redemption_points     INT NOT NULL DEFAULT 1000,
    redemption_value_vnd  DECIMAL(12, 2) NOT NULL DEFAULT 1000.00,
    expiry_month          INT NOT NULL DEFAULT 12,
    expiry_day            INT NOT NULL DEFAULT 31,
    expiry_time           NVARCHAR(8) NOT NULL DEFAULT '23:59:59',
    last_expired_at       DATE,
    created_at            DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at            DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

IF NOT EXISTS (SELECT 1 FROM dbo.loyalty_configurations)
BEGIN
    INSERT INTO dbo.loyalty_configurations
    (
        earning_rate_percent,
        redemption_points,
        redemption_value_vnd,
        expiry_month,
        expiry_day,
        expiry_time,
        created_at,
        updated_at
    )
    VALUES (1.00, 1000, 1000.00, 12, 31, '23:59:59', SYSUTCDATETIME(), SYSUTCDATETIME());
END

IF OBJECT_ID('dbo.loyalty_point_transactions', 'U') IS NULL
CREATE TABLE dbo.loyalty_point_transactions
(
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    booking_id     BIGINT,
    type           NVARCHAR(30) NOT NULL,
    points_delta   INT NOT NULL,
    balance_after  INT NOT NULL,
    note           NVARCHAR(500),
    occurred_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    created_at     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_loyalty_point_transactions_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
    CONSTRAINT fk_loyalty_point_transactions_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings (id)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_loyalty_tx_user' AND object_id = OBJECT_ID('dbo.loyalty_point_transactions'))
CREATE INDEX idx_loyalty_tx_user ON dbo.loyalty_point_transactions (user_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_loyalty_tx_booking' AND object_id = OBJECT_ID('dbo.loyalty_point_transactions'))
CREATE INDEX idx_loyalty_tx_booking ON dbo.loyalty_point_transactions (booking_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_loyalty_tx_type' AND object_id = OBJECT_ID('dbo.loyalty_point_transactions'))
CREATE INDEX idx_loyalty_tx_type ON dbo.loyalty_point_transactions (type);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_loyalty_tx_occurred_at' AND object_id = OBJECT_ID('dbo.loyalty_point_transactions'))
CREATE INDEX idx_loyalty_tx_occurred_at ON dbo.loyalty_point_transactions (occurred_at);
