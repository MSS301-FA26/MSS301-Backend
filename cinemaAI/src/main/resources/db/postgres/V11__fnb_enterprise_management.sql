-- 1. Food Categories
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
);

-- Seed Categories if empty
INSERT INTO food_categories (code, name, description, sort_order, status, created_at, updated_at)
VALUES 
    ('POPCORN', 'Bắp rang', 'Bắp rang bơ, phô mai, caramel nóng giòn', 1, 'ACTIVE', NOW(), NOW()),
    ('DRINK', 'Nước uống', 'Nước ngọt có ga, trà, nước suối đóng chai', 2, 'ACTIVE', NOW(), NOW()),
    ('SNACK', 'Snack & Bánh', 'Khoai tây chiên, snack que, hạt dinh dưỡng', 3, 'ACTIVE', NOW(), NOW()),
    ('HOT_FOOD', 'Thức ăn nóng', 'Xúc xích, cá viên chiên, khoai tây lắc', 4, 'ACTIVE', NOW(), NOW()),
    ('DESSERT', 'Tráng miệng', 'Kem, bánh ngọt, thạch tráng miệng', 5, 'ACTIVE', NOW(), NOW()),
    ('OTHER', 'Khác', 'Các sản phẩm và phụ kiện rạp khác', 6, 'ACTIVE', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- 2. Alter food_items
ALTER TABLE food_items ADD COLUMN IF NOT EXISTS sku VARCHAR(60);
ALTER TABLE food_items ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES food_categories(id);
ALTER TABLE food_items ADD COLUMN IF NOT EXISTS cost_price NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE food_items ADD COLUMN IF NOT EXISTS stock_tracking BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE food_items ADD COLUMN IF NOT EXISTS low_stock_threshold INT NOT NULL DEFAULT 10;
ALTER TABLE food_items ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE food_items ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE food_items ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

-- Backfill SKUs for existing food items
UPDATE food_items SET sku = 'POP-BUTTER' WHERE id = 1 AND (sku IS NULL OR sku = '');
UPDATE food_items SET sku = 'POP-CHEESE' WHERE id = 2 AND (sku IS NULL OR sku = '');
UPDATE food_items SET sku = 'DRK-COCA-L' WHERE id = 3 AND (sku IS NULL OR sku = '');
UPDATE food_items SET sku = 'DRK-PEPSI-L' WHERE id = 4 AND (sku IS NULL OR sku = '');
UPDATE food_items SET sku = 'DRK-WATER-500' WHERE id = 5 AND (sku IS NULL OR sku = '');
UPDATE food_items SET sku = 'DRK-PEACH-TEA' WHERE id = 6 AND (sku IS NULL OR sku = '');
UPDATE food_items SET sku = 'ITEM-' || id WHERE sku IS NULL OR sku = '';

-- Backfill Category for existing food items
UPDATE food_items SET category_id = (SELECT id FROM food_categories WHERE code = 'POPCORN') WHERE id IN (1, 2) AND category_id IS NULL;
UPDATE food_items SET category_id = (SELECT id FROM food_categories WHERE code = 'DRINK') WHERE id IN (3, 4, 5, 6) AND category_id IS NULL;
UPDATE food_items SET category_id = (SELECT id FROM food_categories WHERE code = 'OTHER') WHERE category_id IS NULL;

-- Backfill Cost Prices
UPDATE food_items SET cost_price = ROUND(price * 0.4, 2) WHERE cost_price = 0;

-- Unique constraint on sku for food_items
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_food_items_sku'
    ) THEN
        ALTER TABLE food_items ADD CONSTRAINT uk_food_items_sku UNIQUE (sku);
    END IF;
END $$;

-- 3. Alter food_combos
ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS sku VARCHAR(60);
ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES food_categories(id);
ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS cost_price NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE food_combos ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

UPDATE food_combos SET sku = 'CMB-SOLO' WHERE id = 1 AND (sku IS NULL OR sku = '');
UPDATE food_combos SET sku = 'CMB-COUPLE' WHERE id = 2 AND (sku IS NULL OR sku = '');
UPDATE food_combos SET sku = 'CMB-FAMILY' WHERE id = 3 AND (sku IS NULL OR sku = '');
UPDATE food_combos SET sku = 'CMB-' || id WHERE sku IS NULL OR sku = '';
UPDATE food_combos SET cost_price = ROUND(price * 0.35, 2) WHERE cost_price = 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_food_combos_sku'
    ) THEN
        ALTER TABLE food_combos ADD CONSTRAINT uk_food_combos_sku UNIQUE (sku);
    END IF;
END $$;

-- 4. food_combo_items
CREATE TABLE IF NOT EXISTS food_combo_items (
    id BIGSERIAL PRIMARY KEY,
    combo_id BIGINT NOT NULL REFERENCES food_combos(id) ON DELETE CASCADE,
    food_item_id BIGINT NOT NULL REFERENCES food_items(id),
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_combo_food_item UNIQUE (combo_id, food_item_id)
);

-- Seed combo items for existing combos:
INSERT INTO food_combo_items (combo_id, food_item_id, quantity, created_at, updated_at)
VALUES 
    (1, 1, 1, NOW(), NOW()),
    (1, 3, 1, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO food_combo_items (combo_id, food_item_id, quantity, created_at, updated_at)
VALUES 
    (2, 2, 1, NOW(), NOW()),
    (2, 3, 2, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO food_combo_items (combo_id, food_item_id, quantity, created_at, updated_at)
VALUES 
    (3, 2, 2, NOW(), NOW()),
    (3, 3, 4, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 5. food_inventories
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
);

-- 6. food_inventory_transactions
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
);

CREATE INDEX IF NOT EXISTS idx_food_inv_tx_cinema_food ON food_inventory_transactions (cinema_id, food_item_id);
CREATE INDEX IF NOT EXISTS idx_food_inv_tx_type ON food_inventory_transactions (type);
CREATE INDEX IF NOT EXISTS idx_food_inv_tx_created ON food_inventory_transactions (created_at);

-- 7. food_price_histories
CREATE TABLE IF NOT EXISTS food_price_histories (
    id BIGSERIAL PRIMARY KEY,
    food_item_id BIGINT REFERENCES food_items(id),
    food_combo_id BIGINT REFERENCES food_combos(id),
    old_price NUMERIC(12, 2) NOT NULL,
    new_price NUMERIC(12, 2) NOT NULL,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Initialize stock for existing cinemas and food items:
INSERT INTO food_inventories (cinema_id, food_item_id, quantity, reserved_quantity, low_stock_threshold, created_at, updated_at)
SELECT c.id, f.id, 50, 0, 10, NOW(), NOW()
FROM cinemas c
CROSS JOIN food_items f
ON CONFLICT (cinema_id, food_item_id) DO NOTHING;

-- Record initial IMPORT transaction if transactions table is empty
INSERT INTO food_inventory_transactions (cinema_id, food_item_id, type, quantity, before_quantity, after_quantity, reason, created_by, created_at)
SELECT fi.cinema_id, fi.food_item_id, 'IMPORT', fi.quantity, 0, fi.quantity, 'Khởi tạo tồn kho ban đầu', 'SYSTEM', NOW()
FROM food_inventories fi
WHERE NOT EXISTS (SELECT 1 FROM food_inventory_transactions);

-- Relax status check constraint to support DRAFT, ACTIVE, INACTIVE, ARCHIVED, LOW_STOCK, OUT_OF_STOCK
DO $$
BEGIN
    ALTER TABLE food_items DROP CONSTRAINT IF EXISTS food_items_status_check;
    ALTER TABLE food_items ADD CONSTRAINT food_items_status_check 
        CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED', 'LOW_STOCK', 'OUT_OF_STOCK'));

    ALTER TABLE food_combos DROP CONSTRAINT IF EXISTS food_combos_status_check;
    ALTER TABLE food_combos ADD CONSTRAINT food_combos_status_check 
        CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED', 'LOW_STOCK', 'OUT_OF_STOCK'));
END $$;
