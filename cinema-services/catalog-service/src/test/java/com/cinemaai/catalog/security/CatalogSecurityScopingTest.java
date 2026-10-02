package com.cinemaai.catalog.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "APPLICATION_NAME=catalog-service-test",
        "SERVER_PORT=0",
        "SERVER_ADDRESS=127.0.0.1",
        "DB_URL=jdbc:h2:mem:catalog_security_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "DB_USERNAME=sa", "DB_PASSWORD=", "DB_DRIVER=org.h2.Driver",
        "JPA_DDL_AUTO=validate", "JPA_OPEN_IN_VIEW=false",
        "FLYWAY_ENABLED=true", "FLYWAY_LOCATIONS=classpath:db/migration",
        "MANAGEMENT_ENDPOINTS=health,info", "HEALTH_SHOW_DETAILS=never",
        "INTERNAL_GATEWAY_SECRET=gateway-test-secret",
        "INTERNAL_SERVICE_SECRET=internal-test-secret",
        "JWT_SECRET=test-jwt-secret-key-with-at-least-32-bytes",
        "QUOTE_TTL_SECONDS=300", "LOG_LEVEL_PATTERN=%5p",
        "CLOUDINARY_CLOUD_NAME=test-cloud",
        "CLOUDINARY_API_KEY=test-key",
        "CLOUDINARY_API_SECRET=test-secret",
        "MAX_FILE_SIZE=10MB",
        "MAX_REQUEST_SIZE=10MB"
})
@AutoConfigureMockMvc
class CatalogSecurityScopingTest {

    @Autowired
    private MockMvc mvc;

    private String managerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        var key = Keys.hmacShaKeyFor("test-jwt-secret-key-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
        
        managerToken = Jwts.builder()
                .subject("manager@cinemaai.com")
                .claim("userId", 100L)
                .claim("cinemaId", 1L)
                .claim("roles", List.of("MANAGER"))
                .signWith(key)
                .compact();

        adminToken = Jwts.builder()
                .subject("admin@cinemaai.com")
                .claim("userId", 1L)
                .claim("roles", List.of("ADMIN"))
                .signWith(key)
                .compact();
    }

    @Test
    void manager_cannotMutateMovies() throws Exception {
        mvc.perform(post("/api/v1/admin/movies")
                        .header("X-Gateway-Secret", "gateway-test-secret")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Movie\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void manager_cannotMutateGenres() throws Exception {
        mvc.perform(post("/api/v1/admin/genres")
                        .header("X-Gateway-Secret", "gateway-test-secret")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sci-Fi\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void manager_cannotMutateActors() throws Exception {
        mvc.perform(post("/api/v1/admin/actors")
                        .header("X-Gateway-Secret", "gateway-test-secret")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Actor One\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void manager_cannotMutateDirectors() throws Exception {
        mvc.perform(post("/api/v1/admin/directors")
                        .header("X-Gateway-Secret", "gateway-test-secret")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Director One\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void manager_cannotMutateSystemSettings() throws Exception {
        mvc.perform(post("/api/v1/admin/system-settings")
                        .header("X-Gateway-Secret", "gateway-test-secret")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configKey\":\"test_key\",\"configValue\":\"1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void manager_cannotMutateCinemas() throws Exception {
        mvc.perform(post("/api/v1/admin/cinemas")
                        .header("X-Gateway-Secret", "gateway-test-secret")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Cinema\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalCinemaEndpoint_requiresInternalSecret() throws Exception {
        mvc.perform(get("/internal/v1/cinemas/1"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/internal/v1/cinemas/1")
                        .header("X-Internal-Service-Secret", "internal-test-secret"))
                .andExpect(status().isOk());

        mvc.perform(get("/internal/v1/cinemas/99999")
                        .header("X-Internal-Service-Secret", "internal-test-secret"))
                .andExpect(status().isNotFound());
    }
}
