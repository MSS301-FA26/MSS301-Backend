package com.sba301.cinemaai.config;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Keeps existing PostgreSQL development databases compatible with standalone
 * concession orders while Flyway remains disabled in the current project.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StandaloneFoodOrderSchemaCleanup implements SmartInitializingSingleton {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void afterSingletonsInstantiated() {
        String database = databaseProductName();
        if (!isPostgreSql(database)) {
            return;
        }

        execute("ALTER TABLE food_orders ADD COLUMN IF NOT EXISTS customer_id BIGINT");
        execute("ALTER TABLE food_orders ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP");
        execute("ALTER TABLE food_orders ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP");
        execute("ALTER TABLE food_orders ADD COLUMN IF NOT EXISTS picked_up_at TIMESTAMP");
        execute("ALTER TABLE food_orders DROP CONSTRAINT IF EXISTS food_orders_status_check");
        execute("""
                ALTER TABLE food_orders ADD CONSTRAINT food_orders_status_check
                CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PICKED_UP', 'CANCELLED', 'EXPIRED'))
                """);
        execute("""
                UPDATE food_orders fo
                SET customer_id = b.user_id
                FROM bookings b
                WHERE fo.booking_id = b.id
                  AND fo.customer_id IS NULL
                """);
        execute("""
                UPDATE food_orders
                SET expires_at = created_at + INTERVAL '15 minutes'
                WHERE status = 'PENDING_PAYMENT'
                  AND expires_at IS NULL
                """);
        execute("ALTER TABLE food_orders ALTER COLUMN booking_id DROP NOT NULL");
        execute("ALTER TABLE booking_food_items ALTER COLUMN booking_id DROP NOT NULL");
        execute("ALTER TABLE payments ALTER COLUMN booking_id DROP NOT NULL");
        execute("CREATE INDEX IF NOT EXISTS idx_food_orders_customer ON food_orders (customer_id)");
        execute("CREATE INDEX IF NOT EXISTS idx_food_orders_pending_expiry ON food_orders (status, expires_at)");
        execute("ALTER TABLE food_orders ALTER COLUMN customer_id SET NOT NULL");
    }

    private String databaseProductName() {
        try {
            return jdbcTemplate.execute((ConnectionCallback<String>) connection ->
                    connection.getMetaData().getDatabaseProductName());
        } catch (DataAccessException ex) {
            log.warn("Could not determine database product name: {}", ex.getMessage());
            return "";
        }
    }

    private boolean isPostgreSql(String database) {
        return database != null && database.toLowerCase(Locale.ROOT).contains("postgresql");
    }

    private void execute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ex) {
            log.warn("Could not update standalone food-order schema using SQL: {}\n{}", sql, ex.getMessage());
        }
    }
}
