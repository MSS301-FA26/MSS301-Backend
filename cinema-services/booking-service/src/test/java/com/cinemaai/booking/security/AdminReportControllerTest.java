package com.cinemaai.booking.security;

import com.cinemaai.booking.controller.AdminReportController;
import com.cinemaai.booking.service.AdminAnalyticsService;
import com.cinemaai.booking.service.AdminBookingService;
import com.cinemaai.booking.service.ReportService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminReportController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"APPLICATION_NAME=reports-test", "server.port=0", "server.address=127.0.0.1",
        "app.gateway.secret=gateway-test", "app.internal.secret=internal-test",
        "app.jwt.secret=01234567890123456789012345678901"})
class AdminReportControllerTest {
    @Autowired MockMvc mvc;
    @MockBean AdminAnalyticsService analytics;
    @MockBean AdminBookingService adminBookings;
    @MockBean ReportService reports;
    @MockBean CinemaSecurityService scope;

    String token(String role) {
        return Jwts.builder().subject("7").claim("userId", 7L).claim("roles", List.of(role))
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8))).compact();
    }
    @Test void rejectsAnonymousCustomerAndStaffBeforeLoadingFinancialData() throws Exception {
        mvc.perform(get("/api/v1/admin/reports/analytics").header("X-Gateway-Secret", "gateway-test")).andExpect(status().isUnauthorized());
        for (String role : List.of("USER", "STAFF")) {
            mvc.perform(get("/api/v1/admin/reports/analytics").header("X-Gateway-Secret", "gateway-test")
                    .header("Authorization", "Bearer " + token(role))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(analytics);
    }
    @Test void managerCannotOverrideAuthoritativeCinemaAndDatesBindCorrectly() throws Exception {
        when(scope.resolveEnforcedCinemaId(any(), eq(999L), eq(false))).thenReturn(7L);
        mvc.perform(get("/api/v1/admin/reports/analytics?from=2026-01-01&to=2026-01-31&cinemaId=999")
                .header("X-Gateway-Secret", "gateway-test").header("Authorization", "Bearer " + token("MANAGER"))).andExpect(status().isOk());
        verify(analytics).getAnalytics(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 7L);
    }
    @Test void concessionsUseSameCinemaScope() throws Exception {
        when(scope.resolveEnforcedCinemaId(any(), isNull(), eq(false))).thenReturn(7L);
        mvc.perform(get("/api/v1/admin/reports/concessions?from=2026-01-01&to=2026-01-01")
                .header("X-Gateway-Secret", "gateway-test").header("Authorization", "Bearer " + token("MANAGER"))).andExpect(status().isOk());
        verify(analytics).getConcessions(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), 7L);
    }
}
