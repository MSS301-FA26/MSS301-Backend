package com.cinemaai.catalog.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FlywayConfigurationTest {

    @Test
    void productionExamplesUseFlywayValidationAndHibernateValidation() throws IOException {
        assertThat(readEnvExample("."))
                .contains("JPA_DDL_AUTO=validate")
                .contains("FLYWAY_VALIDATE_ON_MIGRATE=true");
        assertThat(readEnvExample("../booking-service"))
                .contains("JPA_DDL_AUTO=validate");
        assertThat(readEnvExample("../payment-service"))
                .contains("JPA_DDL_AUTO=validate");
    }

    @Test
    void catalogStartupDoesNotRepairMigrationHistoryOrRunSchemaDdl() throws IOException {
        String initializer = Files.readString(Path.of(
                "src/main/java/com/cinemaai/catalog/seeder/CatalogDataInitializer.java"));

        assertThat(Files.exists(Path.of(
                "src/main/java/com/cinemaai/catalog/config/FlywayConfig.java"))).isFalse();
        assertThat(initializer).doesNotContain("ALTER TABLE");
    }

    private String readEnvExample(String serviceDirectory) throws IOException {
        return Files.readString(Path.of(serviceDirectory, ".env.example"));
    }
}
