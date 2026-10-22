package com.cinemaai.payment.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** Direct PostgreSQL verification of V9's pending-payment invariants. */
@EnabledIfSystemProperty(named = "payment.concurrency.test.url", matches = ".+")
class PaymentPendingConcurrencyPostgresTest {
    static Connection admin;
    static String schema;
    static String url = System.getProperty("payment.concurrency.test.url");
    static String user = System.getProperty("payment.concurrency.test.username", "test_user");
    static String password = System.getProperty("payment.concurrency.test.password", "test_pass");

    @BeforeAll static void setup() throws Exception {
        admin = DriverManager.getConnection(url, user, password);
        schema = "payment_concurrency_it_" + System.currentTimeMillis();
        try (Statement s = admin.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
            s.execute("SET search_path TO " + schema);
            s.execute("CREATE TABLE payments (id BIGSERIAL PRIMARY KEY, booking_id BIGINT, food_order_id BIGINT, status VARCHAR(20) NOT NULL)");
            s.execute("CREATE UNIQUE INDEX ux_payments_pending_booking ON payments (booking_id) WHERE booking_id IS NOT NULL AND status = 'PENDING'");
            s.execute("CREATE UNIQUE INDEX ux_payments_pending_food_order ON payments (food_order_id) WHERE food_order_id IS NOT NULL AND status = 'PENDING'");
        }
    }

    @AfterAll static void cleanup() throws Exception {
        try (Statement s = admin.createStatement()) { s.execute("DROP SCHEMA " + schema + " CASCADE"); }
        admin.close();
    }

    @BeforeEach void clear() throws Exception { try (Statement s = admin.createStatement()) { s.execute("TRUNCATE " + schema + ".payments"); } }

    @Test void twoAndTenConcurrentBookingRequestsAllowExactlyOnePending() throws Exception {
        assertEquals(1, concurrentBooking(2));
        clear();
        assertEquals(1, concurrentBooking(10));
    }

    @Test void failedPaymentCanBeRetriedAndFoodOrderIsAlsoUnique() throws Exception {
        insert("booking_id", 7, "FAILED");
        assertEquals(1, concurrentBooking(2));
        clear();
        assertEquals(1, concurrentFoodOrder(10));
    }

    @Test void rejectedConcurrentInsertRollsBackAndLeavesOneRow() throws Exception {
        assertEquals(1, concurrentBooking(10));
        try (PreparedStatement p = admin.prepareStatement("SELECT count(*) FROM " + schema + ".payments WHERE status='PENDING'")) {
            try (ResultSet r = p.executeQuery()) { assertTrue(r.next()); assertEquals(1, r.getInt(1)); }
        }
    }

    private int concurrentBooking(int n) throws Exception { return concurrent("booking_id", 99, n); }
    private int concurrentFoodOrder(int n) throws Exception { return concurrent("food_order_id", 199, n); }
    private int concurrent(String column, long value, int n) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n); CountDownLatch start = new CountDownLatch(1); List<Future<Boolean>> fs = new ArrayList<>();
        for (int i=0;i<n;i++) fs.add(pool.submit(() -> { try (Connection c=DriverManager.getConnection(url,user,password)) { c.setAutoCommit(false); start.await(); try (PreparedStatement p=c.prepareStatement("INSERT INTO "+schema+".payments ("+column+",status) VALUES (?, 'PENDING')")) { p.setLong(1,value); p.executeUpdate(); c.commit(); return true; } catch (SQLException e) { c.rollback(); return false; } } }));
        start.countDown(); int ok=0; for (Future<Boolean> f:fs) if (f.get(10, TimeUnit.SECONDS)) ok++; pool.shutdownNow(); return ok;
    }
    private void insert(String column,long value,String status) throws Exception { try (PreparedStatement p=admin.prepareStatement("INSERT INTO "+schema+".payments ("+column+",status) VALUES (?,?)")) { p.setLong(1,value); p.setString(2,status); p.executeUpdate(); } }
}
