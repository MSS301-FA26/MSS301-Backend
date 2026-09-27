package com.cinemaai.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "APPLICATION_NAME=identity-service-test",
        "SERVER_PORT=0",
        "SERVER_ADDRESS=127.0.0.1",
        "DB_URL=jdbc:h2:mem:identity_app;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "DB_DRIVER=org.h2.Driver",
        "JPA_DDL_AUTO=update",
        "JPA_OPEN_IN_VIEW=false",
        "FLYWAY_ENABLED=false",
        "FLYWAY_LOCATIONS=classpath:db/migration",
        "JWT_SECRET=cineai-development-secret-key-please-change-123456",
        "JWT_ACCESS_EXPIRATION_MS=1800000",
        "JWT_REFRESH_EXPIRATION_MS=604800000",
        "MAIL_ENABLED=false",
        "MAIL_FROM=test@cinemaai.com",
        "MAIL_HOST=localhost",
        "MAIL_PORT=25",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test",
        "MAIL_SMTP_AUTH=false",
        "MAIL_STARTTLS_ENABLE=false",
        "GOOGLE_OAUTH_CLIENT_ID=test-client-id",
        "GOOGLE_TOKEN_INFO_BASE_URL=http://localhost:8080/oauth2",
        "SEED_ADMIN_EMAIL=admin@cinemaai.com",
        "SEED_ADMIN_PASSWORD=Admin123",
        "SEED_ADMIN_FULL_NAME=CinemaAI Admin",
        "SEED_ADMIN_PHONE=0900000001",
        "SEED_STAFF_EMAIL=staff@cinemaai.com",
        "SEED_STAFF_PASSWORD=Staff123",
        "SEED_STAFF_FULL_NAME=CinemaAI Staff",
        "SEED_STAFF_PHONE=0900000002",
        "INTERNAL_GATEWAY_SECRET=gateway-test-secret",
        "INTERNAL_SERVICE_SECRET=internal-test-secret",
        "MANAGEMENT_ENDPOINTS=health,info",
        "HEALTH_SHOW_DETAILS=always",
        "management.health.mail.enabled=false",
        "LOG_LEVEL_PATTERN=%5p"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class IdentityApplicationSmokeTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void directAccessWithoutGatewaySecretIsForbidden() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@cinemaai.com\",\"password\":\"password\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Trusted service access required"));
    }

    @Test
    void accessWithGatewaySecretPassesSecretCheck() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .header("X-Gateway-Secret", "gateway-test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@cinemaai.com\",\"password\":\"password\"}"))
                // Passes secret check, proceeds to auth logic (returns 400 or 401, not 403 secret rejection)
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        403, result.getResponse().getStatus()));
    }

    @Test
    void actuatorHealthIsWhitelistedWithoutSecret() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void correlationIdIsPropagated() throws Exception {
        mvc.perform(get("/actuator/health")
                        .header("X-Correlation-Id", "test-corr-id-12345"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "test-corr-id-12345"));
    }
}
