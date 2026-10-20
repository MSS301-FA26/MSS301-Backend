package com.cinemaai.booking.client;

import com.cinemaai.booking.exception.ForbiddenException;
import com.cinemaai.booking.exception.NotFoundException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogClientShowtimeScopeTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsInternalSecretAndReturnsTrustedScope() throws Exception {
        AtomicReference<String> secret = new AtomicReference<>();
        server = server(200, "{\"success\":true,\"data\":{\"showtimeId\":9,\"cinemaId\":101},\"message\":\"ok\",\"timestamp\":null}", secret);
        CatalogClient client = client(1000, 1000);

        assertEquals(101L, client.getShowtimeScope(9L).cinemaId());
        assertEquals("internal-test", secret.get());
    }

    @Test
    void nullCinemaAndMismatchedShowtimeFailClosed() throws Exception {
        server = server(200, "{\"success\":true,\"data\":{\"showtimeId\":8,\"cinemaId\":null},\"message\":\"ok\",\"timestamp\":null}", new AtomicReference<>());
        CatalogClient client = client(1000, 1000);

        assertThrows(ForbiddenException.class, () -> client.getShowtimeScope(9L));
    }

    @Test
    void notFoundIsPreservedAndTimeoutFailsClosed() throws Exception {
        server = server(404, "{}", new AtomicReference<>());
        CatalogClient client = client(1000, 1000);
        assertThrows(NotFoundException.class, () -> client.getShowtimeScope(404L));
        server.stop(0);
        server = null;

        CatalogClient unavailable = new CatalogClient("http://127.0.0.1:1", "internal-test", 50, 50);
        assertThrows(ForbiddenException.class, () -> unavailable.getShowtimeScope(9L));
    }

    private HttpServer server(int status, String body, AtomicReference<String> secret) throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/internal/v1/catalog/showtimes/9/scope", exchange -> {
            secret.set(exchange.getRequestHeaders().getFirst("X-Internal-Service-Secret"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        httpServer.createContext("/internal/v1/catalog/showtimes/404/scope", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        httpServer.start();
        return httpServer;
    }

    private CatalogClient client(int connectTimeout, int readTimeout) {
        return new CatalogClient("http://127.0.0.1:" + server.getAddress().getPort(),
                "internal-test", connectTimeout, readTimeout);
    }
}
