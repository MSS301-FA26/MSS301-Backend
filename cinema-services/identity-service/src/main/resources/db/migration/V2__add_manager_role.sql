-- ==============================================================================
-- FLYWAY V2: ADD MANAGER ROLE (PostgreSQL)
-- ==============================================================================

-- 1. Update check constraint on roles table if present
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check;
ALTER TABLE roles ADD CONSTRAINT roles_name_check CHECK (name IN ('ADMIN', 'MANAGER', 'CUSTOMER', 'STAFF'));

-- 2. Insert MANAGER role
INSERT INTO roles (name, created_at, updated_at)
VALUES ('MANAGER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;
