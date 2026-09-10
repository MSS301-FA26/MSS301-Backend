IF COL_LENGTH('dbo.showtimes', 'adult_standard_price') IS NULL
ALTER TABLE dbo.showtimes ADD adult_standard_price DECIMAL(12, 2) NULL;

IF COL_LENGTH('dbo.showtimes', 'child_standard_price') IS NULL
ALTER TABLE dbo.showtimes ADD child_standard_price DECIMAL(12, 2) NULL;

IF COL_LENGTH('dbo.showtimes', 'student_standard_price') IS NULL
ALTER TABLE dbo.showtimes ADD student_standard_price DECIMAL(12, 2) NULL;

IF COL_LENGTH('dbo.showtimes', 'adult_vip_price') IS NULL
ALTER TABLE dbo.showtimes ADD adult_vip_price DECIMAL(12, 2) NULL;

IF COL_LENGTH('dbo.showtimes', 'child_vip_price') IS NULL
ALTER TABLE dbo.showtimes ADD child_vip_price DECIMAL(12, 2) NULL;

IF COL_LENGTH('dbo.showtimes', 'student_vip_price') IS NULL
ALTER TABLE dbo.showtimes ADD student_vip_price DECIMAL(12, 2) NULL;

IF COL_LENGTH('dbo.showtimes', 'adult_couple_price') IS NULL
ALTER TABLE dbo.showtimes ADD adult_couple_price DECIMAL(12, 2) NULL;

IF COL_LENGTH('dbo.showtimes', 'child_couple_price') IS NULL
ALTER TABLE dbo.showtimes ADD child_couple_price DECIMAL(12, 2) NULL;

IF COL_LENGTH('dbo.showtimes', 'student_couple_price') IS NULL
ALTER TABLE dbo.showtimes ADD student_couple_price DECIMAL(12, 2) NULL;

IF COL_LENGTH('dbo.showtimes', 'weekend_surcharge') IS NULL
ALTER TABLE dbo.showtimes ADD weekend_surcharge BIT NOT NULL CONSTRAINT df_showtimes_weekend_surcharge DEFAULT 0;

IF COL_LENGTH('dbo.showtimes', 'holiday_surcharge') IS NULL
ALTER TABLE dbo.showtimes ADD holiday_surcharge BIT NOT NULL CONSTRAINT df_showtimes_holiday_surcharge DEFAULT 0;

UPDATE dbo.showtimes
SET
    adult_standard_price = COALESCE(adult_standard_price, base_price),
    child_standard_price = COALESCE(child_standard_price, base_price),
    student_standard_price = COALESCE(student_standard_price, base_price),
    adult_vip_price = COALESCE(adult_vip_price, vip_price, base_price + 20000),
    child_vip_price = COALESCE(child_vip_price, vip_price, base_price + 20000),
    student_vip_price = COALESCE(student_vip_price, vip_price, base_price + 20000),
    adult_couple_price = COALESCE(adult_couple_price, couple_price, base_price + 30000),
    child_couple_price = COALESCE(child_couple_price, couple_price, base_price + 30000),
    student_couple_price = COALESCE(student_couple_price, couple_price, base_price + 30000);
