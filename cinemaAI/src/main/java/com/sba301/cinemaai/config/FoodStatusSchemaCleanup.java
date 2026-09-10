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
public class FoodStatusSchemaCleanup implements SmartInitializingSingleton {

    private static final String FOOD_STATUS_CHECK = "status IN ('ACTIVE', 'LOW_STOCK', 'INACTIVE', 'OUT_OF_STOCK')";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void afterSingletonsInstantiated() {
        String database = databaseProductName();
        if (isPostgreSql(database)) {
            updatePostgresFoodStatusConstraints();
            return;
        }
        log.warn("Skipping food status schema cleanup for unsupported database: {}", database);
    }

    private void updatePostgresFoodStatusConstraints() {
        execute("""
                DO $$
                DECLARE
                    constraint_record RECORD;
                BEGIN
                    FOR constraint_record IN
                        SELECT n.nspname, t.relname, c.conname
                        FROM pg_constraint c
                        JOIN pg_class t ON t.oid = c.conrelid
                        JOIN pg_namespace n ON n.oid = t.relnamespace
                        JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
                        WHERE t.relname IN ('food_items', 'food_combos')
                          AND c.contype = 'c'
                          AND a.attname = 'status'
                    LOOP
                        EXECUTE format(
                            'ALTER TABLE %I.%I DROP CONSTRAINT %I',
                            constraint_record.nspname,
                            constraint_record.relname,
                            constraint_record.conname
                        );
                    END LOOP;
                END $$
                """);
        execute("ALTER TABLE food_items ADD CONSTRAINT food_items_status_check CHECK (" + FOOD_STATUS_CHECK + ")");
        execute("ALTER TABLE food_combos ADD CONSTRAINT food_combos_status_check CHECK (" + FOOD_STATUS_CHECK + ")");
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
            log.warn("Could not execute food status schema cleanup SQL: {}\n{}", sql, ex.getMessage());
        }
    }
}
