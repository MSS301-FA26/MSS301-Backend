IF COL_LENGTH('dbo.showtimes', 'late_night_surcharge_amount') IS NULL
ALTER TABLE dbo.showtimes ADD late_night_surcharge_amount DECIMAL(12, 2) NOT NULL CONSTRAINT df_showtimes_late_night_surcharge_amount DEFAULT 20000;
