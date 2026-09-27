package com.cinemaai.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "APPLICATION_NAME=catalog-service-test",
        "SERVER_PORT=0",
        "SERVER_ADDRESS=127.0.0.1",
        "DB_URL=jdbc:h2:mem:catalog_app;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class CatalogApplicationSmokeTest {
    @Autowired MockMvc mvc;

    @Test
    void publicCatalogIsAvailableOnlyThroughTrustedGateway() throws Exception {
        mvc.perform(get("/api/v1/movies").header("X-Gateway-Secret", "gateway-test-secret"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/movies"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCrudRequiresJwtAndInternalQuoteRequiresServiceSecret() throws Exception {
        mvc.perform(post("/api/v1/admin/movies")
                        .header("X-Gateway-Secret", "gateway-test-secret")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/v1/catalog/checkout-quote")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreateMovieWithJwt() throws Exception {
        var key = io.jsonwebtoken.security.Keys.hmacShaKeyFor("test-jwt-secret-key-with-at-least-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String token = io.jsonwebtoken.Jwts.builder()
                .subject("admin@cinemaai.com")
                .claim("roles", java.util.List.of("ADMIN"))
                .signWith(key)
                .compact();

        mvc.perform(post("/api/v1/admin/movies")
                        .header("X-Gateway-Secret", "gateway-test-secret")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerScheduleEndpointIsAccessible() throws Exception {
        mvc.perform(get("/api/v1/showtimes/customer-schedule")
                        .header("X-Gateway-Secret", "gateway-test-secret"))
                .andExpect(status().isOk());
    }
}
