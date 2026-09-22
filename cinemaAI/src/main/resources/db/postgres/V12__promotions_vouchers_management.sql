-- V12: Promotions & Vouchers Management Schema

CREATE TABLE IF NOT EXISTS promotions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    discount_type VARCHAR(30) NOT NULL DEFAULT 'PERCENTAGE',
    discount_value NUMERIC(12, 2) NOT NULL DEFAULT 0,
    min_order_value NUMERIC(12, 2) NOT NULL DEFAULT 0,
    max_discount_amount NUMERIC(12, 2),
    usage_limit INT,
    used_count INT NOT NULL DEFAULT 0,
    user_usage_limit INT NOT NULL DEFAULT 1,
    applicable_target VARCHAR(30) NOT NULL DEFAULT 'ALL',
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS promotion_usages (
    id BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT NOT NULL REFERENCES promotions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    booking_id BIGINT REFERENCES bookings(id),
    food_order_id BIGINT REFERENCES food_orders(id),
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    used_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_promotions_code ON promotions(code);
CREATE INDEX IF NOT EXISTS idx_promotions_status ON promotions(status);
CREATE INDEX IF NOT EXISTS idx_promotions_dates ON promotions(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_promo_usages_promo_user ON promotion_usages(promotion_id, user_id);

-- Seed initial promotions
INSERT INTO promotions (code, name, description, discount_type, discount_value, min_order_value, max_discount_amount, usage_limit, used_count, user_usage_limit, applicable_target, start_date, end_date, status, created_at, updated_at)
VALUES 
    ('CINE20', 'Ưu đãi Giảm 20% Toàn Rạp', 'Giảm 20% tối đa 50.000đ cho đơn hàng từ 100.000đ', 'PERCENTAGE', 20.00, 100000.00, 50000.00, 500, 12, 1, 'ALL', NOW() - INTERVAL '7 day', NOW() + INTERVAL '90 day', 'ACTIVE', NOW(), NOW()),
    ('POPFREE15K', 'Voucher Bắp Nước 15.000đ', 'Giảm ngay 15.000đ khi mua bắp rang hoặc combo F&B từ 50.000đ', 'FIXED_AMOUNT', 15000.00, 50000.00, 15000.00, 200, 5, 2, 'FOOD_ONLY', NOW() - INTERVAL '5 day', NOW() + INTERVAL '60 day', 'ACTIVE', NOW(), NOW()),
    ('VIP50K', 'Đặc quyền Thành viên VIP 50K', 'Giảm trực tiếp 50.000đ cho các đơn hàng lớn từ 200.000đ', 'FIXED_AMOUNT', 50000.00, 200000.00, 50000.00, 100, 8, 1, 'ALL', NOW() - INTERVAL '10 day', NOW() + INTERVAL '45 day', 'ACTIVE', NOW(), NOW()),
    ('SUMMER10', 'Chào Hè Rực Rỡ Giảm 10%', 'Ưu đãi hè giảm 10% không giới hạn trần giảm giá', 'PERCENTAGE', 10.00, 0.00, NULL, 1000, 34, 3, 'ALL', NOW() - INTERVAL '2 day', NOW() + INTERVAL '120 day', 'ACTIVE', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;
