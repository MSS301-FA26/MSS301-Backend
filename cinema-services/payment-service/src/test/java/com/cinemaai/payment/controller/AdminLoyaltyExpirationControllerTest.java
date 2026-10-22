package com.cinemaai.payment.controller;

import com.cinemaai.payment.security.SecurityConfig;
import com.cinemaai.payment.service.LoyaltyService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminLoyaltyController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "APPLICATION_NAME=loyalty-expiration-test",
        "server.address=127.0.0.1",
        "server.port=0",
        "app.gateway.secret=gateway-test",
        "app.internal.secret=internal-test",
        "app.jwt.secret=01234567890123456789012345678901"
})
class AdminLoyaltyExpirationControllerTest {
    private static final String PATH = "/api/v1/admin/loyalty/points/expire-all-active";
    private static final String JWT_KEY = "01234567890123456789012345678901";

    @Autowired MockMvc mvc;
    @MockBean LoyaltyService loyaltyService;

    @Test
    void newPostRouteDelegatesAndReturnsAffectedCountForAdmin() throws Exception {
        when(loyaltyService.expireAllActivePoints("ADMIN")).thenReturn(3);

        mvc.perform(post(PATH)
                        .header("X-Gateway-Secret", "gateway-test")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3))
                .andExpect(jsonPath("$.message").value("Đã cho hết hạn điểm thưởng của 3 tài khoản"));

        verify(loyaltyService).expireAllActivePoints("ADMIN");
    }

    @Test
    void oldRouteIsNoLongerMapped() throws Exception {
        mvc.perform(post("/api/v1/admin/loyalty/expire-now")
                        .header("X-Gateway-Secret", "gateway-test")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void newRouteAcceptsPostOnly() throws Exception {
        mvc.perform(get(PATH)
                        .header("X-Gateway-Secret", "gateway-test")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(post(PATH).header("X-Gateway-Secret", "gateway-test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedNonAdminIsForbidden() throws Exception {
        mvc.perform(post(PATH)
                        .header("X-Gateway-Secret", "gateway-test")
                        .header("Authorization", "Bearer " + token("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    private String token(String role) {
        return Jwts.builder()
                .subject("admin@example.test")
                .claim("userId", 7L)
                .claim("roles", List.of(role))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(JWT_KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
