package com.cinemaai.booking.security;

import com.cinemaai.booking.controller.AdminBookingController;
import com.cinemaai.booking.controller.StaffCheckInController;
import com.cinemaai.booking.dto.response.ShowtimeBookingSummaryDto;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.BookingSeatRepository;
import com.cinemaai.booking.repository.FoodOrderRepository;
import com.cinemaai.booking.service.AdminBookingService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminBookingController.class, StaffCheckInController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "APPLICATION_NAME=security-test",
        "server.address=127.0.0.1",
        "server.port=0",
        "app.gateway.secret=gateway-test",
        "app.internal.secret=internal-test",
        "app.jwt.secret=01234567890123456789012345678901"
})
class ProductionControllerAuthorizationTest {
    private static final String KEY = "01234567890123456789012345678901";

    @Autowired MockMvc mvc;
    @MockBean AdminBookingService adminBookingService;
    @MockBean BookingRepository bookingRepository;
    @MockBean BookingSeatRepository bookingSeatRepository;
    @MockBean FoodOrderRepository foodOrderRepository;
    @MockBean CinemaSecurityService cinemaSecurityService;
    @MockBean BookingMapper bookingMapper;

    @Test
    void productionAdminControllerRejectsUserAndAllowsAdmin() throws Exception {
        when(adminBookingService.getShowtimeBookingSummary(any(), eq(9L)))
                .thenReturn(new ShowtimeBookingSummaryDto(9L, 0, 0, 0, BigDecimal.ZERO));

        mvc.perform(get("/api/v1/admin/bookings/showtimes/9/summary")
                        .header("X-Gateway-Secret", "gateway-test")
                        .header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/bookings/showtimes/9/summary")
                        .header("X-Gateway-Secret", "gateway-test")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void productionStaffControllerRejectsUserAndAllowsStaff() throws Exception {
        when(cinemaSecurityService.resolveEnforcedCinemaId(any(), isNull(), eq(true))).thenReturn(101L);
        when(bookingRepository.findRecentForCheckInByCinema(anyList(), eq(101L), any())).thenReturn(List.of());

        mvc.perform(get("/api/v1/staff/check-in/recent")
                        .header("X-Gateway-Secret", "gateway-test")
                        .header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/staff/check-in/recent")
                        .header("X-Gateway-Secret", "gateway-test")
                        .header("Authorization", "Bearer " + token("STAFF")))
                .andExpect(status().isOk());
    }

    private String token(String role) {
        return Jwts.builder().subject("actor@test.com")
                .claim("userId", 7L)
                .claim("roles", List.of(role))
                .claim("cinemaId", 101L)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
