package com.cinemaai.payment.client;

import com.cinemaai.payment.exception.NotFoundException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingClientFailureTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void distinguishesNotFoundFromDownstreamServerFailure() throws Exception {
        BookingClient notFound = clientResponding(404, "missing");
        assertThrows(NotFoundException.class, () -> notFound.getBooking(7L));

        server.stop(0);
        BookingClient serverFailure = clientResponding(500, "failed");
        assertThrows(IllegalStateException.class, () -> serverFailure.getBooking(7L));
    }

    @Test
    void rejectsInvalidResponseAndPropagatesMarkPaidFailure() throws Exception {
        BookingClient invalid = clientResponding(200, "{}");
        assertThrows(IllegalStateException.class, () -> invalid.getBooking(7L));

        server.stop(0);
        BookingClient markPaidFailure = clientResponding(503, "offline");
        assertThrows(IllegalStateException.class,
                () -> markPaidFailure.markBookingPaid(7L, "txn", java.time.LocalDateTime.now()));
    }

    private BookingClient clientResponding(int status, String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return new BookingClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-secret");
    }
}
