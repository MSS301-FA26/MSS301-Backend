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

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var result = connection.createStatement().executeQuery(
                     "select count(*) from information_schema.tables where table_schema = 'public' and table_name = 'showtimes'")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
    }
}
