package com.sba301.cinemaai.config;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionSchemaInitializer implements SmartInitializingSingleton {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void afterSingletonsInstantiated() {
        String database = databaseProductName();
        if (!isPostgreSql(database)) {
            return;
        }

        log.info("Initializing Promotions & Vouchers schema and default seed data...");

        execute("""
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
                )
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS promotion_usages (
                    id BIGSERIAL PRIMARY KEY,
                    promotion_id BIGINT NOT NULL REFERENCES promotions(id),
                    user_id BIGINT NOT NULL REFERENCES users(id),
                    booking_id BIGINT REFERENCES bookings(id),
                    food_order_id BIGINT REFERENCES food_orders(id),
                    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
                    used_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

        // Seed initial promotions
        execute("""
                INSERT INTO promotions (code, name, description, discount_type, discount_value, min_order_value, max_discount_amount, usage_limit, used_count, user_usage_limit, applicable_target, start_date, end_date, status, created_at, updated_at)
                VALUES 
                    ('CINE20', 'Ưu đãi Giảm 20% Toàn Rạp', 'Giảm 20% tối đa 50.000đ cho đơn hàng từ 100.000đ', 'PERCENTAGE', 20.00, 100000.00, 50000.00, 500, 12, 1, 'ALL', NOW() - INTERVAL '7 day', NOW() + INTERVAL '90 day', 'ACTIVE', NOW(), NOW()),
                    ('POPFREE15K', 'Voucher Bắp Nước 15.000đ', 'Giảm ngay 15.000đ khi mua bắp rang hoặc combo F&B từ 50.000đ', 'FIXED_AMOUNT', 15000.00, 50000.00, 15000.00, 200, 5, 2, 'FOOD_ONLY', NOW() - INTERVAL '5 day', NOW() + INTERVAL '60 day', 'ACTIVE', NOW(), NOW()),
                    ('VIP50K', 'Đặc quyền Thành viên VIP 50K', 'Giảm trực tiếp 50.000đ cho các đơn hàng lớn từ 200.000đ', 'FIXED_AMOUNT', 50000.00, 200000.00, 50000.00, 100, 8, 1, 'ALL', NOW() - INTERVAL '10 day', NOW() + INTERVAL '45 day', 'ACTIVE', NOW(), NOW()),
                    ('SUMMER10', 'Chào Hè Rực Rỡ Giảm 10%', 'Ưu đãi hè giảm 10% không giới hạn trần giảm giá', 'PERCENTAGE', 10.00, 0.00, NULL, 1000, 34, 3, 'ALL', NOW() - INTERVAL '2 day', NOW() + INTERVAL '120 day', 'ACTIVE', NOW(), NOW())
                ON CONFLICT (code) DO NOTHING
                """);

        log.info("Promotions & Vouchers schema initialized successfully.");
    }

    private void execute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ex) {
            log.warn("PromotionSchemaInitializer SQL warning: {}", ex.getMessage());
        }
    }

    private String databaseProductName() {
        try {
            return jdbcTemplate.execute((ConnectionCallback<String>) connection ->
                    connection.getMetaData().getDatabaseProductName());
        } catch (DataAccessException ex) {
            return "";
        }
    }

    private boolean isPostgreSql(String databaseProductName) {
        return databaseProductName != null
                && databaseProductName.toLowerCase(Locale.ROOT).contains("postgresql");
    }
}
