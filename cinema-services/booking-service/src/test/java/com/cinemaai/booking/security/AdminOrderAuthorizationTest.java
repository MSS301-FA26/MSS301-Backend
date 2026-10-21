package com.cinemaai.booking.security;

import com.cinemaai.booking.controller.AdminOrderController;
import com.cinemaai.booking.dto.response.PageResponse;
import com.cinemaai.booking.service.OrderService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminOrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "APPLICATION_NAME=security-test",
        "server.address=127.0.0.1",
        "server.port=0",
        "app.gateway.secret=gateway-test",
        "app.internal.secret=internal-test",
        "app.jwt.secret=01234567890123456789012345678901"
})
class AdminOrderAuthorizationTest {
    private static final String KEY = "01234567890123456789012345678901";

    @Autowired MockMvc mvc;
    @MockBean OrderService orderService;

    @Test
    void adminCanListFoodOrders() throws Exception {
        when(orderService.listAll(any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mvc.perform(get("/api/v1/admin/food-orders")
                        .header("X-Gateway-Secret", "gateway-test")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void customerAndManagerCannotListAllFoodOrders() throws Exception {
        for (String role : List.of("CUSTOMER", "MANAGER")) {
            mvc.perform(get("/api/v1/admin/food-orders")
                            .header("X-Gateway-Secret", "gateway-test")
                            .header("Authorization", "Bearer " + token(role)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/food-orders")
                        .header("X-Gateway-Secret", "gateway-test"))
                .andExpect(status().isUnauthorized());
    }

    private String token(String role) {
        return Jwts.builder().subject("actor@test.com")
                .claim("userId", 7L)
                .claim("roles", List.of(role))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
