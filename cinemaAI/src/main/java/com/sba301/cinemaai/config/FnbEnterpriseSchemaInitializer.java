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
public class FnbEnterpriseSchemaInitializer implements SmartInitializingSingleton {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void afterSingletonsInstantiated() {
        String database = databaseProductName();
        if (!isPostgreSql(database)) {
            return;
        }

        log.info("Initializing F&B enterprise schema, categories, combo items, and inventory constraints...");

        // 1. Categories
        execute("""
                CREATE TABLE IF NOT EXISTS food_categories (
                    id BIGSERIAL PRIMARY KEY,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    description VARCHAR(500),
                    image_url VARCHAR(500),
                    sort_order INT NOT NULL DEFAULT 0,
                    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                    deleted_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

        execute("""
                INSERT INTO food_categories (code, name, description, sort_order, status, created_at, updated_at)
                VALUES 
                    ('POPCORN', 'Bắp rang', 'Bắp rang bơ, phô mai, caramel nóng giòn', 1, 'ACTIVE', NOW(), NOW()),
                    ('DRINK', 'Nước uống', 'Nước ngọt có ga, trà, nước suối đóng chai', 2, 'ACTIVE', NOW(), NOW()),
                    ('SNACK', 'Snack & Bánh', 'Khoai tây chiên, snack que, hạt dinh dưỡng', 3, 'ACTIVE', NOW(), NOW()),
                    ('HOT_FOOD', 'Thức ăn nóng', 'Xúc xích, cá viên chiên, khoai tây lắc', 4, 'ACTIVE', NOW(), NOW()),
                    ('DESSERT', 'Tráng miệng', 'Kem, bánh ngọt, thạch tráng miệng', 5, 'ACTIVE', NOW(), NOW()),
                    ('OTHER', 'Khác', 'Các sản phẩm và phụ kiện rạp khác', 6, 'ACTIVE', NOW(), NOW())
                ON CONFLICT (code) DO NOTHING
                """);

        // 2. food_items columns
        execute("ALTER TABLE food_items ADD COLUMN IF NOT EXISTS sku VARCHAR(60)");
        execute("ALTER TABLE food_items ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES food_categories(id)");
        execute("ALTER TABLE food_items ADD COLUMN IF NOT EXISTS cost_price NUMERIC(12, 2) NOT NULL DEFAULT 0");
        execute("ALTER TABLE food_items ADD COLUMN IF NOT EXISTS stock_tracking BOOLEAN NOT NULL DEFAULT TRUE");
        execute("ALTER TABLE food_items ADD COLUMN IF NOT EXISTS low_stock_threshold INT NOT NULL DEFAULT 10");
        execute("ALTER TABLE food_items ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP");
        execute("ALTER TABLE food_items ADD COLUMN IF NOT EXISTS created_by VARCHAR(100)");
        execute("ALTER TABLE food_items ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100)");

        execute("UPDATE food_items SET sku = 'ITEM-' || id WHERE sku IS NULL OR sku = ''");
        execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_food_items_sku') THEN
                        ALTER TABLE food_items ADD CONSTRAINT uk_food_items_sku UNIQUE (sku);
                    END IF;
                END $$;
                """);

        // 3. food_combos columns
        execute("ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS sku VARCHAR(60)");
        execute("ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES food_categories(id)");
        execute("ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS cost_price NUMERIC(12, 2) NOT NULL DEFAULT 0");
        execute("ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP");
        execute("ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS created_by VARCHAR(100)");
        execute("ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100)");

        execute("UPDATE food_combos SET sku = 'CMB-' || id WHERE sku IS NULL OR sku = ''");
        execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_food_combos_sku') THEN
                        ALTER TABLE food_combos ADD CONSTRAINT uk_food_combos_sku UNIQUE (sku);
                    END IF;
                END $$;
                """);

        // 4. food_combo_items
        execute("""
                CREATE TABLE IF NOT EXISTS food_combo_items (
                    id BIGSERIAL PRIMARY KEY,
                    combo_id BIGINT NOT NULL REFERENCES food_combos(id) ON DELETE CASCADE,
                    food_item_id BIGINT NOT NULL REFERENCES food_items(id),
                    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    CONSTRAINT uk_combo_food_item UNIQUE (combo_id, food_item_id)
                )
                """);

        // 5. food_inventories
        execute("""
                CREATE TABLE IF NOT EXISTS food_inventories (
                    id BIGSERIAL PRIMARY KEY,
                    cinema_id BIGINT NOT NULL REFERENCES cinemas(id),
                    food_item_id BIGINT NOT NULL REFERENCES food_items(id),
                    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
                    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
                    low_stock_threshold INT NOT NULL DEFAULT 10,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    CONSTRAINT uk_cinema_food_inventory UNIQUE (cinema_id, food_item_id)
                )
                """);

        // 6. food_inventory_transactions
        execute("""
                CREATE TABLE IF NOT EXISTS food_inventory_transactions (
                    id BIGSERIAL PRIMARY KEY,
                    cinema_id BIGINT NOT NULL REFERENCES cinemas(id),
                    food_item_id BIGINT NOT NULL REFERENCES food_items(id),
                    type VARCHAR(30) NOT NULL,
                    quantity INT NOT NULL,
                    before_quantity INT NOT NULL,
                    after_quantity INT NOT NULL,
                    reason VARCHAR(500) NOT NULL,
                    reference_id VARCHAR(100),
                    created_by VARCHAR(100),
                    created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

        // 7. food_price_histories
        execute("""
                CREATE TABLE IF NOT EXISTS food_price_histories (
                    id BIGSERIAL PRIMARY KEY,
                    food_item_id BIGINT REFERENCES food_items(id),
                    food_combo_id BIGINT REFERENCES food_combos(id),
                    old_price NUMERIC(12, 2) NOT NULL,
                    new_price NUMERIC(12, 2) NOT NULL,
                    changed_by VARCHAR(100),
                    changed_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

        // 8. Relax status check constraints
        execute("""
                DO $$
                BEGIN
                    ALTER TABLE food_items DROP CONSTRAINT IF EXISTS food_items_status_check;
                    ALTER TABLE food_items ADD CONSTRAINT food_items_status_check 
                        CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED', 'LOW_STOCK', 'OUT_OF_STOCK'));

                    ALTER TABLE food_combos DROP CONSTRAINT IF EXISTS food_combos_status_check;
                    ALTER TABLE food_combos ADD CONSTRAINT food_combos_status_check 
                        CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED', 'LOW_STOCK', 'OUT_OF_STOCK'));
                END $$;
                """);

        log.info("F&B enterprise schema initialized successfully.");
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
            log.warn("Could not execute F&B schema init SQL: {}\n{}", sql, ex.getMessage());
        }
    }
}
