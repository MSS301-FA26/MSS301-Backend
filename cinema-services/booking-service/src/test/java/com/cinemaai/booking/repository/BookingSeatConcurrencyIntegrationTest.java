package com.cinemaai.booking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
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

class BookingSeatConcurrencyIntegrationTest {

    private static final long SHOWTIME_ID = 9001L;
    private static final long SEAT_ID = 42L;
    private static final String DATABASE = "booking_test";
    private static final String USERNAME = "booking_test";
    private static final String PASSWORD = "booking_test";
    private static final String CONTAINER_NAME = "booking-seat-test-" + UUID.randomUUID();
    private static String jdbcUrl;

    @BeforeAll
    static void startPostgresAndMigrateSchema() throws Exception {
        runDocker(
                "run", "--rm", "--detach", "--publish-all", "--name", CONTAINER_NAME,
                "--env", "POSTGRES_DB=" + DATABASE,
                "--env", "POSTGRES_USER=" + USERNAME,
                "--env", "POSTGRES_PASSWORD=" + PASSWORD,
                "postgres:16-alpine"
        );
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
    void allowsOnlyOneActiveHoldForTheSameShowtimeAndSeatUnderConcurrentTransactions() throws Exception {
        CountDownLatch transactionsReady = new CountDownLatch(2);
        CountDownLatch startInserts = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> attempts = List.of(
                    executor.submit(createConcurrentHold(transactionsReady, startInserts)),
                    executor.submit(createConcurrentHold(transactionsReady, startInserts))
            );

            assertEquals(true, transactionsReady.await(10, TimeUnit.SECONDS), "Both transactions must be ready");
            startInserts.countDown();

            long successfulHolds = 0;
            for (Future<Boolean> attempt : attempts) {
                if (attempt.get(10, TimeUnit.SECONDS)) {
                    successfulHolds++;
                }
            }

            assertEquals(1, successfulHolds);
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM booking_seats
                     WHERE showtime_id = ? AND seat_id = ? AND status = 'HOLDING'
                     """)) {
            statement.setLong(1, SHOWTIME_ID);
            statement.setLong(2, SEAT_ID);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                assertEquals(1, result.getLong(1));
            }
        }
    }

    private Callable<Boolean> createConcurrentHold(
            CountDownLatch transactionsReady,
            CountDownLatch startInserts
    ) {
        return () -> {
            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                long bookingId = insertBooking(connection);
                transactionsReady.countDown();
                startInserts.await(10, TimeUnit.SECONDS);
                insertHoldingSeat(connection, bookingId);
                connection.commit();
                return true;
            } catch (Exception exception) {
                return false;
            }
        };
    }

    private long insertBooking(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO bookings (
                    booking_code, user_id, showtime_id, movie_id, movie_title_snapshot,
                    cinema_name_snapshot, room_name_snapshot, showtime_start_snapshot,
                    subtotal, discount_amount, loyalty_points_redeemed, total_amount,
                    status, hold_expires_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setString(1, "TEST-" + UUID.randomUUID());
            statement.setLong(2, 1L);
            statement.setLong(3, SHOWTIME_ID);
            statement.setLong(4, 1L);
            statement.setString(5, "Test movie");
            statement.setString(6, "Test cinema");
            statement.setString(7, "Test room");
            statement.setTimestamp(8, Timestamp.valueOf(now.plusDays(1)));
            statement.setBigDecimal(9, BigDecimal.ZERO);
            statement.setBigDecimal(10, BigDecimal.ZERO);
            statement.setInt(11, 0);
            statement.setBigDecimal(12, BigDecimal.ZERO);
            statement.setString(13, "HOLDING");
            statement.setTimestamp(14, Timestamp.valueOf(now.plusMinutes(3)));
            statement.setTimestamp(15, Timestamp.valueOf(now));
            statement.setTimestamp(16, Timestamp.valueOf(now));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void insertHoldingSeat(Connection connection, long bookingId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO booking_seats (
                    booking_id, showtime_id, seat_id, row_label, seat_number, seat_type,
                    unit_price, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setLong(1, bookingId);
            statement.setLong(2, SHOWTIME_ID);
            statement.setLong(3, SEAT_ID);
            statement.setString(4, "A");
            statement.setInt(5, 1);
            statement.setString(6, "STANDARD");
            statement.setBigDecimal(7, BigDecimal.ZERO);
            statement.setString(8, "HOLDING");
            statement.setTimestamp(9, Timestamp.valueOf(now));
            statement.setTimestamp(10, Timestamp.valueOf(now));
            statement.executeUpdate();
        }
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
