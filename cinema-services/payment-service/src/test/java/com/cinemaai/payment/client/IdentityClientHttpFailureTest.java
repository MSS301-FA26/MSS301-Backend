package com.cinemaai.payment.client;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Controlled HTTP faults, not evidence of a running Identity application. */
class IdentityClientHttpFailureTest {
    @Test void downstream4xxAnd5xxCannotGrantScopeOrProfile() throws Exception {
        for (int status : new int[]{403, 404, 500, 503}) {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                assertEquals("test-only-internal", exchange.getRequestHeaders().getFirst("X-Internal-Service-Secret"));
                exchange.sendResponseHeaders(status, -1);
                exchange.close();
            });
            server.start();
            try {
                var client = new IdentityClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-only-internal");
                assertNull(client.getUserAccessScope(1L), "HTTP " + status);
                assertNull(client.getUserProfile(1L), "HTTP " + status);
            } finally { server.stop(0); }
        }
    }

    @Test void readTimeoutFailsClosedWithinBoundedTime() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            try { Thread.sleep(5000); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        try {
            var client = new IdentityClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-only-internal");
            assertTimeoutPreemptively(Duration.ofSeconds(8), () -> assertNull(client.getUserAccessScope(1L)));
        } finally { server.stop(0); executor.shutdownNow(); }
    }
}
