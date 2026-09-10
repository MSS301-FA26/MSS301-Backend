package com.sba301.cinemaai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewSchemaCleanup {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void allowOneReviewPerUsedTicket() {
        String database = databaseProductName();
        if (isPostgreSql(database)) {
            dropPostgresUniqueIndexesOnUserMovieFromReviews();
            createPostgresUniqueIndexOnBookingId();
            return;
        }
        if (isMySql(database)) {
            dropMySqlUniqueIndexesOnUserMovieFromReviews();
            createMySqlUniqueIndexOnBookingId();
            return;
        }
        log.warn("Skipping review schema cleanup for unsupported database: {}", database);
    }

    private void dropPostgresUniqueIndexesOnUserMovieFromReviews() {
        execute("ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_user_id_movie_id_key");
        execute("ALTER TABLE reviews DROP CONSTRAINT IF EXISTS uk_reviews_user_movie");
        execute("""
                DO $$
                DECLARE
                    constraint_record RECORD;
                BEGIN
                    FOR constraint_record IN
                        SELECT n.nspname, c.conname
                        FROM pg_constraint c
                        JOIN pg_class t ON t.oid = c.conrelid
                        JOIN pg_namespace n ON n.oid = t.relnamespace
                        WHERE t.relname = 'reviews'
                          AND c.contype = 'u'
                          AND (
                              SELECT array_agg(a.attname::text ORDER BY a.attname::text)
                              FROM unnest(c.conkey) key(attnum)
                              JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = key.attnum
                          ) = ARRAY['movie_id', 'user_id']
                    LOOP
                        EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT %I', constraint_record.nspname, 'reviews', constraint_record.conname);
                    END LOOP;
                END $$
                """);
        execute("""
                DO $$
                DECLARE
                    index_record RECORD;
                BEGIN
                    FOR index_record IN
                        SELECT schemaname, indexname
                        FROM pg_indexes
                        WHERE tablename = 'reviews'
                          AND indexdef ILIKE '%unique%'
                          AND indexdef ILIKE '%user_id%'
                          AND indexdef ILIKE '%movie_id%'
                    LOOP
                        EXECUTE format('DROP INDEX IF EXISTS %I.%I', index_record.schemaname, index_record.indexname);
                    END LOOP;
                END $$
                """);
    }

    private void createPostgresUniqueIndexOnBookingId() {
        execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_reviews_booking_active
                ON reviews(booking_id)
                WHERE booking_id IS NOT NULL AND status <> 'DELETED'
                """);
    }

    private void dropMySqlUniqueIndexesOnUserMovieFromReviews() {
        try {
            List<String> indexes = jdbcTemplate.queryForList("""
                    SELECT DISTINCT INDEX_NAME
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'reviews'
                      AND NON_UNIQUE = 0
                      AND INDEX_NAME IN (
                          SELECT INDEX_NAME FROM information_schema.STATISTICS
                          WHERE TABLE_SCHEMA = DATABASE()
                            AND TABLE_NAME = 'reviews'
                            AND COLUMN_NAME = 'user_id'
                      )
                      AND INDEX_NAME IN (
                          SELECT INDEX_NAME FROM information_schema.STATISTICS
                          WHERE TABLE_SCHEMA = DATABASE()
                            AND TABLE_NAME = 'reviews'
                            AND COLUMN_NAME = 'movie_id'
                      )
                      AND INDEX_NAME <> 'PRIMARY'
                    """, String.class);

            for (String indexName : indexes) {
                execute("ALTER TABLE reviews DROP INDEX `" + indexName + "`");
            }
        } catch (DataAccessException ex) {
            log.warn("Could not query review indexes: {}", ex.getMessage());
        }
    }

    private void createMySqlUniqueIndexOnBookingId() {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'reviews'
                      AND INDEX_NAME = 'uk_reviews_booking_active'
                    """, Integer.class);
            if (count == null || count == 0) {
                execute("CREATE UNIQUE INDEX uk_reviews_booking_active ON reviews(booking_id)");
            }
        } catch (DataAccessException ex) {
            log.warn("Could not create review booking index: {}", ex.getMessage());
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
            log.warn("Could not execute review schema cleanup SQL: {}\n{}", sql, ex.getMessage());
        }
    }
}
