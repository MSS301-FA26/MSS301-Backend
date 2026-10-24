package com.cinemaai.gateway;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

/** Actual application.properties routes; downstreams are contract probes, not AI integration. */
class ConfiguredGatewayHttpTest {
    @Test void configuredRoutesChooseTargetPreservePathsAndSetOneTrustedSecret() throws Exception {
        var servers = new ArrayList<HttpServer>();
        String[] names = {"IDENTITY", "CATALOG", "BOOKING", "PAYMENT", "RECOMMENDATION"};
        var args = new ArrayList<String>(java.util.List.of(
                "--spring.config.import=file:.env.example[.properties]", "--server.port=0", "--server.address=127.0.0.1",
                "--INTERNAL_GATEWAY_SECRET=java-test", "--AI_GATEWAY_SECRET=ai-test", "--logging.level.root=WARN"));
        try {
            for (String name : names) {
                var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                server.createContext("/", exchange -> {
                    var headers = exchange.getRequestHeaders();
                    String body = name + "|" + exchange.getRequestURI() + "|" + headers.get("X-Gateway-Secret")
                            + "|" + headers.getFirst("X-User-Id") + "|" + headers.getFirst("X-Internal-Service-Secret");
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
                server.start(); servers.add(server);
                args.add("--" + name + "_SERVICE_URI=http://127.0.0.1:" + server.getAddress().getPort());
            }
            try (var context = new SpringApplicationBuilder(ApiGatewayApplication.class).run(args.toArray(String[]::new))) {
                int port = ((ReactiveWebServerApplicationContext) context).getWebServer().getPort();
                var client = HttpClient.newHttpClient();
                String[][] cases = {
                        {"/api/v1/staff/profiles/42", "IDENTITY", "/api/v1/staff/profiles/42", "java-test"},
                        {"/api/v1/admin/audit-logs", "IDENTITY", "/api/v1/admin/audit-logs", "java-test"},
                        {"/api/v1/movies", "CATALOG", "/api/v1/movies", "java-test"},
                        {"/api/v1/bookings", "BOOKING", "/api/v1/bookings", "java-test"},
                        {"/api/v1/wallet", "PAYMENT", "/api/v1/wallet", "java-test"},
                        {"/api/v1/search/adaptive", "RECOMMENDATION", "/api/v1/search/adaptive", "ai-test"},
                        {"/api/v1/chat", "RECOMMENDATION", "/api/v1/chat/message", "ai-test"},
                        {"/api/v1/recommendation/content/7", "RECOMMENDATION", "/api/v1/recommendations/movies/7/similar", "ai-test"},
                        {"/api/v1/recommendation/collaborative/8", "RECOMMENDATION", "/api/v1/recommendations/users/8", "ai-test"}
                };
                for (var sample : cases) {
                    var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + sample[0]))
                            .timeout(Duration.ofSeconds(10)).header("X-Gateway-Secret", "spoof")
                            .header("X-User-Id", "1").header("X-Internal-Service-Secret", "spoof").GET().build();
                    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, response.statusCode(), sample[0]);
                    assertEquals(sample[1] + "|" + sample[2] + "|[" + sample[3] + "]|null|null", response.body());
                }
                var internal = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/internal/v1/users/1")).GET().build();
                assertEquals(404, client.send(internal, HttpResponse.BodyHandlers.ofString()).statusCode());
            }
        } finally { servers.forEach(server -> server.stop(0)); }
    }
}
