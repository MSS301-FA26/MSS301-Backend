package com.sba301.cinemaai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketPricingSchemaCleanup {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void removeSeniorTicketSupport() {
        String database = databaseProductName();
        execute("DELETE FROM ticket_pricing_rules WHERE ticket_type = 'SENIOR'");

        if (isPostgreSql(database)) {
            execute("ALTER TABLE ticket_pricing_rules ADD COLUMN IF NOT EXISTS seat_type VARCHAR(30)");
        } else if (isMySql(database)) {
            addMySqlColumnIfNotExists("ticket_pricing_rules", "seat_type", "VARCHAR(30)");
        } else {
            log.warn("Skipping ticket pricing schema column cleanup for unsupported database: {}", database);
        }

        execute("UPDATE ticket_pricing_rules SET seat_type = 'STANDARD' WHERE seat_type IS NULL");

        execute("DELETE FROM ticket_pricing_rules WHERE seat_type = 'COUPLE' AND ticket_type <> 'ADULT'");

        if (isPostgreSql(database)) {
            execute("ALTER TABLE ticket_combos DROP COLUMN IF EXISTS senior_count");
        } else if (isMySql(database)) {
            dropMySqlColumnIfExists("ticket_combos", "senior_count");
        }
    }

    private void addMySqlColumnIfNotExists(String table, String column, String definition) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                      AND COLUMN_NAME = ?
                    """, Integer.class, table, column);
            if (count == null || count == 0) {
                execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
            }
        } catch (DataAccessException ex) {
            log.warn("Could not check/add column {}.{}: {}", table, column, ex.getMessage());
        }
    }

    private void dropMySqlColumnIfExists(String table, String column) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                      AND COLUMN_NAME = ?
                    """, Integer.class, table, column);
            if (count != null && count > 0) {
                execute("ALTER TABLE `" + table + "` DROP COLUMN `" + column + "`");
            }
        } catch (DataAccessException ex) {
            log.warn("Could not check/drop column {}.{}: {}", table, column, ex.getMessage());
        }
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

    private boolean isMySql(String database) {
        if (database == null) {
            return false;
        }
        String normalized = database.toLowerCase(Locale.ROOT);
        return normalized.contains("mysql") || normalized.contains("mariadb");
    }

    private void execute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ex) {
            log.warn("Could not execute ticket pricing schema cleanup SQL: {}\n{}", sql, ex.getMessage());
        }
    }
}
