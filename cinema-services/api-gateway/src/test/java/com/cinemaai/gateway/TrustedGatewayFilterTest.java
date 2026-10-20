package com.cinemaai.gateway;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;
class TrustedGatewayFilterTest {
 @Test void sanitizesIdentityAndPreservesBearerAcrossRoutes() {
  var filter = new TrustedGatewayFilter("gateway", "ai");
  for (String path : new String[]{"/api/v1/auth/login", "/api/v1/users/me", "/api/v1/bookings", "/api/v1/payments", "/api/v1/movies", "/api/v1/chat", "/api/v1/recommendations"}) {
   var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).header("X-User-Id", "9").header("X-User-Roles", "ADMIN").header("X-User-Email", "fake").header("X-User-Cinema-Id", "5").header("X-Internal-Service-Secret", "fake").header("Authorization", "Bearer token").build());
   AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
   filter.filter(exchange, e -> { forwarded.set(e); return Mono.empty(); }).block();
   var h = forwarded.get().getRequest().getHeaders();
   assertNull(h.getFirst("X-User-Id")); assertNull(h.getFirst("X-User-Roles")); assertNull(h.getFirst("X-User-Email")); assertNull(h.getFirst("X-User-Cinema-Id")); assertNull(h.getFirst("X-Internal-Service-Secret"));
   assertEquals("Bearer token", h.getFirst("Authorization")); assertEquals((path.endsWith("chat") || path.endsWith("recommendations")) ? "ai" : "gateway", h.getFirst("X-Gateway-Secret"));
  }
 }
 @Test void internalRouteIsNotForwarded() {
  var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/internal/probe").build());
  new TrustedGatewayFilter("gateway", "ai").filter(exchange, e -> { fail("forwarded"); return Mono.empty(); }).block();
  assertEquals(404, exchange.getResponse().getStatusCode().value());
 }
}
