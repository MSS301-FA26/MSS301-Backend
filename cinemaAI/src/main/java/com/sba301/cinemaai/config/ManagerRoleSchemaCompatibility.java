package com.sba301.cinemaai.config;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Keeps existing PostgreSQL databases compatible while Flyway is not wired into the runtime. */
@Component
@RequiredArgsConstructor
public class ManagerRoleSchemaCompatibility implements SmartInitializingSingleton {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void afterSingletonsInstantiated() {
        String database = jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            return metadata.getDatabaseProductName();
        });
        if (!"PostgreSQL".equalsIgnoreCase(database)) return;
        jdbcTemplate.execute("ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check");
        jdbcTemplate.execute("ALTER TABLE roles ADD CONSTRAINT roles_name_check "
                + "CHECK (name IN ('ADMIN', 'MANAGER', 'STAFF', 'CUSTOMER'))");
    }
}
