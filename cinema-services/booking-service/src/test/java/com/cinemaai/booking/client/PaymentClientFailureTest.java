package com.cinemaai.booking.client;

import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentClientFailureTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void propagatesUnknownLoyaltyAwardOutcomeForRetry() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = "offline".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        PaymentClient client = new PaymentClient(
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-secret");

        assertThrows(IllegalStateException.class,
                () -> client.awardLoyaltyPoints(1L, 7L, "BK7", BigDecimal.TEN, 0, 2L));
    }
}
