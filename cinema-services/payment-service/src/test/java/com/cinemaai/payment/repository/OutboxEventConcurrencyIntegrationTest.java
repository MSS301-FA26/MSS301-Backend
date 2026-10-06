package com.cinemaai.payment.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OutboxEventConcurrencyIntegrationTest {

    private static final String DATABASE = "payment_test";
    private static final String USERNAME = "payment_test";
    private static final String PASSWORD = "payment_test";
    private static final String DEDUPLICATION_KEY = "payment-succeeded-9001";
    private static final String CONTAINER_NAME = "payment-outbox-test-" + UUID.randomUUID();
    private static String jdbcUrl;

    @BeforeAll
    static void startPostgresAndMigrateSchema() throws Exception {
        runDocker(
                "run", "--rm", "--detach", "--publish-all", "--name", CONTAINER_NAME,
                "--env", "POSTGRES_DB=" + DATABASE,
                "--env", "POSTGRES_USER=" + USERNAME,
                "--env", "POSTGRES_PASSWORD=" + PASSWORD,
                "postgres:16-alpine");
        waitForPostgres();
        jdbcUrl = "jdbc:postgresql://localhost:" + postgresPort() + "/" + DATABASE;
        Flyway.configure()
                .dataSource(jdbcUrl, USERNAME, PASSWORD)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @AfterAll
    static void stopPostgres() throws Exception {
        runDocker("rm", "--force", CONTAINER_NAME);
    }

    @Test
    void allowsOnlyOnePaymentSucceededOutboxEventUnderConcurrentInserts() throws Exception {
        CountDownLatch attemptsReady = new CountDownLatch(2);
        CountDownLatch startInserts = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> attempts = List.of(
                    executor.submit(insertEvent(attemptsReady, startInserts)),
                    executor.submit(insertEvent(attemptsReady, startInserts)));

            assertEquals(true, attemptsReady.await(10, TimeUnit.SECONDS));
            startInserts.countDown();

            long successfulInserts = 0;
            for (Future<Boolean> attempt : attempts) {
                if (attempt.get(10, TimeUnit.SECONDS)) {
                    successfulInserts++;
                }
            }
            assertEquals(1, successfulInserts);
        }

        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM outbox_events WHERE deduplication_key = ?")) {
            statement.setString(1, DEDUPLICATION_KEY);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                assertEquals(1, result.getLong(1));
            }
        }
    }

    private Callable<Boolean> insertEvent(CountDownLatch attemptsReady, CountDownLatch startInserts) {
        return () -> {
            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                attemptsReady.countDown();
                startInserts.await(10, TimeUnit.SECONDS);
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO outbox_events (
                            aggregate_type, aggregate_id, event_type, deduplication_key,
                            payload, status, retry_count, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    statement.setString(1, "PAYMENT");
                    statement.setString(2, "9001");
                    statement.setString(3, "PaymentSucceededEvent");
                    statement.setString(4, DEDUPLICATION_KEY);
                    statement.setString(5, "{}");
                    statement.setString(6, "PENDING");
                    statement.setInt(7, 0);
                    statement.setObject(8, LocalDateTime.now());
                    statement.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (Exception exception) {
                return false;
            }
        };
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD);
    }

    private static void waitForPostgres() throws Exception {
        for (int attempt = 0; attempt < 30; attempt++) {
            if (runDockerAllowFailure("exec", CONTAINER_NAME, "pg_isready", "-U", USERNAME, "-d", DATABASE) == 0) {
                return;
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("PostgreSQL test container did not become ready");
    }

    private static int postgresPort() throws Exception {
        String output = runDocker("port", CONTAINER_NAME, "5432/tcp").trim();
        return Integer.parseInt(output.substring(output.lastIndexOf(':') + 1));
    }

    private static String runDocker(String... arguments) throws Exception {
        Process process = new ProcessBuilder(concat("docker", arguments))
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        if (process.waitFor() != 0) {
            throw new IllegalStateException("Docker command failed: " + output);
        }
        return output;
    }

    private static int runDockerAllowFailure(String... arguments) throws Exception {
        Process process = new ProcessBuilder(concat("docker", arguments))
                .redirectErrorStream(true)
                .start();
        process.getInputStream().readAllBytes();
        return process.waitFor();
    }

    private static String[] concat(String first, String... rest) {
        String[] command = new String[rest.length + 1];
        command[0] = first;
        System.arraycopy(rest, 0, command, 1, rest.length);
        return command;
    }
}
