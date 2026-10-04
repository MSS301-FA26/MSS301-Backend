package com.cinemaai.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class CatalogMigrationTest {
    @Test
    void initializesCatalogSchemaOnCleanDatabase() throws Exception {
        String url = "jdbc:h2:mem:catalog_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            assertCountAtLeast(connection, "cinemas", 1);
            assertCountAtLeast(connection, "rooms", 1);
            assertCountAtLeast(connection, "seat_rows", 1);
            assertCountAtLeast(connection, "seats", 1);
            assertCountAtLeast(connection, "showtimes", 1);
            assertCountAtLeast(connection, "ticket_pricing_rules", 1);
        }
    }

    private void assertCountAtLeast(java.sql.Connection connection, String table, int expectedMinimum) throws Exception {
        try (var result = connection.createStatement().executeQuery("select count(*) from " + table)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isGreaterThanOrEqualTo(expectedMinimum);
        }
    }
}
