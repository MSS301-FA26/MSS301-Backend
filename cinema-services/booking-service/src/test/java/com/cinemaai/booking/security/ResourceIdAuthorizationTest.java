package com.cinemaai.booking.security;

import com.cinemaai.booking.controller.BookingController;
import com.cinemaai.booking.controller.OrderController;
import com.cinemaai.booking.exception.NotFoundException;
import com.cinemaai.booking.service.BookingService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {BookingController.class, OrderController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "APPLICATION_NAME=resource-security-test",
        "server.address=127.0.0.1",
        "server.port=0",
        "app.gateway.secret=gateway-test",
        "app.internal.secret=internal-test",
        "app.jwt.secret=01234567890123456789012345678901"
})
class ResourceIdAuthorizationTest {
    private static final String KEY = "01234567890123456789012345678901";

    @Autowired MockMvc mvc;
    @MockBean BookingService bookingService;
    @MockBean OrderService orderService;
    @MockBean(name = "bookingSecurity") BookingSecurity bookingSecurity;
    @MockBean(name = "orderSecurity") OrderSecurity orderSecurity;

    @Test
    void ownerCanReadButOtherUserAndUnknownResourcesAreHidden() throws Exception {
        when(bookingSecurity.isOwner(101L, 7L)).thenReturn(true);
        when(orderSecurity.isOwner(201L, 7L)).thenReturn(true);

        mvc.perform(get("/api/v1/bookings/101").headers(headers(7L, "USER"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/food-orders/201").headers(headers(7L, "USER"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/bookings/102").headers(headers(7L, "USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/food-orders/202").headers(headers(7L, "USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/bookings/999").headers(headers(7L, "USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/food-orders/999").headers(headers(7L, "USER"))).andExpect(status().isForbidden());
    }

    @Test
    void zeroNegativeAndNonNumericIdsFollowCurrentContract() throws Exception {
        mvc.perform(get("/api/v1/bookings/0").headers(headers(7L, "USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/bookings/-1").headers(headers(7L, "USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/food-orders/0").headers(headers(7L, "USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/food-orders/-1").headers(headers(7L, "USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/bookings/not-a-number").headers(headers(7L, "USER"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/food-orders/not-a-number").headers(headers(7L, "USER"))).andExpect(status().isBadRequest());
    }

    @Test
    void invalidJwtIsRejectedBeforeResourceLookup() throws Exception {
        mvc.perform(get("/api/v1/bookings/101").header("X-Gateway-Secret", "gateway-test")
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/food-orders/201").header("X-Gateway-Secret", "gateway-test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminGetsNotFoundForMissingResources() throws Exception {
        when(bookingService.getBookingByIdInternal(999L)).thenThrow(new NotFoundException("missing"));
        when(orderService.getById(999L)).thenThrow(new NotFoundException("missing"));

        mvc.perform(get("/api/v1/bookings/999").headers(headers(9L, "ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/food-orders/999").headers(headers(9L, "ADMIN"))).andExpect(status().isNotFound());
    }

    @Test
    void adminUsesUnscopedLookupForExistingResources() throws Exception {
        mvc.perform(get("/api/v1/bookings/101").headers(headers(9L, "ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/food-orders/201").headers(headers(9L, "ADMIN"))).andExpect(status().isOk());

        verify(bookingService).getBookingByIdInternal(101L);
        verify(orderService).getById(201L);
    }

    private org.springframework.http.HttpHeaders headers(Long userId, String role) {
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Gateway-Secret", "gateway-test");
        headers.setBearerAuth(Jwts.builder().subject("user@test.com")
                .claim("userId", userId)
                .claim("roles", List.of(role))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8)))
                .compact());
        return headers;
    }
}
