package com.cinemaai.catalog.security;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogGatewayCompatibilityTest.Probe.class)
@Import({SecurityConfig.class, CatalogGatewayCompatibilityTest.Probe.class})
@TestPropertySource(properties={"APPLICATION_NAME=security-test", "LOG_LEVEL_PATTERN=%5p", "server.port=0", "server.address=127.0.0.1", "app.gateway.secret=gateway-test", "app.internal.secret=internal-test", "app.jwt.secret=01234567890123456789012345678901"})
class CatalogGatewayCompatibilityTest {
 @Autowired MockMvc mvc;
 String token(String role) { return Jwts.builder().subject("a@example.com").claim("userId", 7L).claim("roles", List.of(role)).expiration(new Date(System.currentTimeMillis()+60000)).signWith(Keys.hmacShaKeyFor("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8))).compact(); }
 @Test void publicAndJwtProtectedAndAdminRoutesWorkWithSanitizedGatewayHeaders() throws Exception {
  mvc.perform(get("/api/v1/movies/probe").header("X-Gateway-Secret","gateway-test")).andExpect(status().isOk());
  mvc.perform(post("/api/v1/reviews/probe").header("X-Gateway-Secret","gateway-test")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/reviews/probe").header("X-Gateway-Secret","gateway-test").header("Authorization","Bearer " + token("USER"))).andExpect(status().isOk()).andExpect(content().string("7:[ROLE_USER]"));
  mvc.perform(post("/api/v1/admin/movies/probe").header("X-Gateway-Secret","gateway-test").header("Authorization","Bearer " + token("USER"))).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/admin/movies/probe").header("X-Gateway-Secret","gateway-test").header("Authorization","Bearer " + token("ADMIN"))).andExpect(status().isOk());
 }
 @RestController static class Probe {
  @GetMapping("/api/v1/movies/probe") String publicRoute(){return "ok";}
  @PostMapping("/api/v1/reviews/probe") String protectedRoute(@AuthenticationPrincipal AuthenticatedUser u){return u.id()+":"+u.authorities();}
  @PostMapping("/api/v1/admin/movies/probe") String admin(){return "ok";}
 }
}
