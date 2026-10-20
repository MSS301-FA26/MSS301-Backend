package com.cinemaai.booking.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SecurityRegressionTest.Probe.class)
@Import({SecurityConfig.class, SecurityRegressionTest.Probe.class})
@TestPropertySource(properties = {"APPLICATION_NAME=security-test", "server.address=127.0.0.1", "server.port=0", "app.gateway.secret=gateway-test", "app.internal.secret=internal-test", "app.jwt.secret=01234567890123456789012345678901"})
class SecurityRegressionTest {
    @Autowired MockMvc mvc;
    static final String KEY = "01234567890123456789012345678901";
    String token(String key, long expiration) {
        return Jwts.builder().subject("a@example.com").claim("userId", 7L).claim("roles", List.of("USER"))
            .expiration(new Date(expiration)).signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();
    }
    String valid() { return token(KEY, System.currentTimeMillis() + 60000); }
    @Test void missingAndForgedIdentityRejected() throws Exception {
        mvc.perform(get("/probe").header("X-Gateway-Secret", "gateway-test").header("X-User-Id", "9").header("X-User-Roles", "ADMIN")).andExpect(status().isUnauthorized());
    }
    @Test void invalidAndExpiredRejected() throws Exception {
        mvc.perform(get("/probe").header("X-Gateway-Secret", "gateway-test").header("Authorization", "Bearer " + token("abcdefghijklmnopqrstuvwxyz123456", System.currentTimeMillis()+60000))).andExpect(status().isUnauthorized());
        mvc.perform(get("/probe").header("X-Gateway-Secret", "gateway-test").header("Authorization", "Bearer " + token(KEY, System.currentTimeMillis()-60000))).andExpect(status().isUnauthorized());
    }
    @Test void signedIdentityOverridesForgedHeaders() throws Exception {
        mvc.perform(get("/probe").header("X-Gateway-Secret", "gateway-test").header("Authorization", "Bearer " + valid()).header("X-User-Id", "9").header("X-User-Roles", "ADMIN"))
          .andExpect(status().isOk()).andExpect(content().string("7:[ROLE_USER]"));
    }
    @Test void missingExpirationAndUserIdAreRejected() throws Exception {
        var key = Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8));
        String noExp = Jwts.builder().claim("userId", 7L).signWith(key).compact();
        String noId = Jwts.builder().subject("a@example.com").expiration(new Date(System.currentTimeMillis()+60000)).signWith(key).compact();
        String badId = Jwts.builder().claim("userId", 0L).expiration(new Date(System.currentTimeMillis()+60000)).signWith(key).compact();
        for (String token : List.of(noExp, noId, badId))
            mvc.perform(get("/probe").header("X-Gateway-Secret", "gateway-test").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
    @Test void internalSecretRequired() throws Exception {
        mvc.perform(get("/internal/probe")).andExpect(status().isForbidden());
        mvc.perform(get("/internal/probe").header("X-Internal-Service-Secret", "wrong")).andExpect(status().isForbidden());
        mvc.perform(get("/internal/probe").header("X-Internal-Service-Secret", "internal-test")).andExpect(status().isOk());
    }
    @RestController static class Probe {
        @GetMapping("/probe") String probe(@AuthenticationPrincipal AuthenticatedUser user) { return user.id() + ":" + user.authorities(); }
        @GetMapping("/internal/probe") String internal() { return "ok"; }
    }
}
