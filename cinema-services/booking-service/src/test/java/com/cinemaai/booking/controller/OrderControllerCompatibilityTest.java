package com.cinemaai.booking.controller;

import com.cinemaai.booking.security.SecurityConfig;
import com.cinemaai.booking.security.OrderSecurity;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrderController.class, InternalOrderController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "APPLICATION_NAME=order-controller-test",
        "server.address=127.0.0.1",
        "server.port=0",
        "app.gateway.secret=gateway-test",
        "app.internal.secret=internal-test",
        "app.jwt.secret=01234567890123456789012345678901"
})
class OrderControllerCompatibilityTest {
    private static final String KEY = "01234567890123456789012345678901";
    private static final String ORDER_BODY = """
            {"foods":[{"productId":11,"isCombo":false,"quantity":2}]}
            """;

    @Autowired MockMvc mvc;
    @MockBean OrderService orderService;
    @MockBean(name = "orderSecurity") OrderSecurity orderSecurity;

    @Test
    void allCustomerRoutesKeepTheirMethodsStatusesAndAuthenticatedIdentity() throws Exception {
        when(orderService.listMine(7L)).thenReturn(List.of());
        when(orderService.listByBooking(7L, 31L)).thenReturn(List.of());
        when(orderSecurity.isOwner(41L, 7L)).thenReturn(true);

        mvc.perform(post("/api/v1/food-orders")
                        .headers(customerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/food-orders/my").headers(customerHeaders()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/food-orders/41").headers(customerHeaders()))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/food-orders/41").headers(customerHeaders()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/food-orders/41/sync").headers(customerHeaders()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/bookings/31/food-orders")
                        .headers(customerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/bookings/31/food-orders").headers(customerHeaders()))
                .andExpect(status().isOk());

        verify(orderService).createStandalone(eq(7L), any());
        verify(orderService).listMine(7L);
        verify(orderService).getByIdAndUser(7L, 41L);
        verify(orderService).cancel(7L, 41L);
        verify(orderService).syncPaymentStatus(7L, 41L);
        verify(orderService).createForBooking(eq(7L), eq(31L), any());
        verify(orderService).listByBooking(7L, 31L);
    }

    @Test
    void publicRoutesRequireGatewayAndAuthenticatedCustomer() throws Exception {
        mvc.perform(get("/api/v1/food-orders/my")
                        .header("X-Gateway-Secret", "gateway-test"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/food-orders/my")
                        .header("Authorization", "Bearer " + token("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalRoutesAcceptOnlyInternalServiceSecret() throws Exception {
        mvc.perform(get("/internal/v1/food-orders/41"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/internal/v1/food-orders/41")
                        .header("X-Gateway-Secret", "gateway-test"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/internal/v1/food-orders/41")
                        .header("X-Internal-Service-Secret", "internal-test"))
                .andExpect(status().isOk());
        mvc.perform(post("/internal/v1/food-orders/41/mark-paid")
                        .header("X-Internal-Service-Secret", "internal-test")
                        .param("transactionId", "TX-41"))
                .andExpect(status().isOk());

        verify(orderService).getById(41L);
        verify(orderService).markPaid(41L, "TX-41");
    }

    private org.springframework.http.HttpHeaders customerHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Gateway-Secret", "gateway-test");
        headers.setBearerAuth(token("CUSTOMER"));
        return headers;
    }

    private String token(String role) {
        return Jwts.builder().subject("customer@test.com")
                .claim("userId", 7L)
                .claim("roles", List.of(role))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
