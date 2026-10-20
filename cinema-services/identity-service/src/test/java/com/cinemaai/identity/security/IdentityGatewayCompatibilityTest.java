package com.cinemaai.identity.security;
import com.cinemaai.identity.config.JwtProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IdentityGatewayCompatibilityTest.Probe.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, IdentityGatewayCompatibilityTest.Probe.class})
@TestPropertySource(properties={"APPLICATION_NAME=security-test", "LOG_LEVEL_PATTERN=%5p", "app.mail.enabled=false", "server.port=0", "server.address=127.0.0.1", "app.gateway.secret=gateway-test", "app.internal.secret=internal-test", "app.jwt.secret=01234567890123456789012345678901", "app.jwt.access-expiration-ms=60000", "app.jwt.refresh-expiration-ms=60000"})
class IdentityGatewayCompatibilityTest {
 @Autowired MockMvc mvc;
 @Autowired JwtService jwt;
 @MockBean CustomUserDetailsService users;
 @MockBean org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;
 @Test void publicAndProtectedRoutesWithForwardedJwt() throws Exception {
  when(users.loadUserByUsername("a@example.com")).thenReturn(User.withUsername("a@example.com").password("x").roles("USER").build());
  mvc.perform(post("/api/v1/auth/login").header("X-Gateway-Secret","gateway-test")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/users/probe").header("X-Gateway-Secret","gateway-test")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/users/probe").header("X-Gateway-Secret","gateway-test").header("Authorization", "Bearer " + jwt.generateAccessToken(7L,"a@example.com",List.of("USER"))))
   .andExpect(status().isOk()).andExpect(content().string("a@example.com:[ROLE_USER]"));
  mvc.perform(get("/api/v1/admin/users").header("X-Gateway-Secret","gateway-test").header("Authorization", "Bearer " + jwt.generateAccessToken(7L,"a@example.com",List.of("ADMIN"))))
   .andExpect(status().isForbidden());
 }
 @RestController static class Probe {
  @PostMapping("/api/v1/auth/login") String login(){return "ok";}
  @GetMapping("/api/v1/users/probe") String protectedRoute(Authentication a){return a.getName()+":"+a.getAuthorities();}
  @GetMapping("/api/v1/admin/users") String admin(){return "ok";}
 }
}
