package com.cinemaai.catalog.security;

import com.cinemaai.catalog.controller.InternalShowtimeController;
import com.cinemaai.catalog.dto.response.cinema.ReportingShowtimeResponse;
import com.cinemaai.catalog.entity.Cinema;
import com.cinemaai.catalog.entity.Room;
import com.cinemaai.catalog.entity.Showtime;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.service.ShowtimeService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InternalShowtimeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "APPLICATION_NAME=security-test",
        "server.address=127.0.0.1",
        "server.port=0",
        "LOG_LEVEL_PATTERN=%5p",
        "app.gateway.secret=gateway-test",
        "app.internal.secret=internal-test",
        "app.jwt.secret=01234567890123456789012345678901"
})
class InternalShowtimeControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean ShowtimeService showtimeService;

    @Test
    void missingAndInvalidInternalSecretAreRejectedBeforeLookup() throws Exception {
        mvc.perform(get("/internal/v1/catalog/showtimes/9/scope"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/internal/v1/catalog/showtimes/9/scope")
                        .header("X-Internal-Service-Secret", "wrong"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(showtimeService);
    }

    @Test
    void validInternalSecretReturnsTrustedShowtimeCinemaMapping() throws Exception {
        Showtime showtime = mock(Showtime.class);
        Room room = mock(Room.class);
        Cinema cinema = mock(Cinema.class);
        when(showtimeService.findById(9L)).thenReturn(showtime);
        when(showtime.getId()).thenReturn(9L);
        when(showtime.getRoom()).thenReturn(room);
        when(room.getCinema()).thenReturn(cinema);
        when(cinema.getId()).thenReturn(101L);

        mvc.perform(get("/internal/v1/catalog/showtimes/9/scope")
                        .header("X-Internal-Service-Secret", "internal-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.showtimeId").value(9))
                .andExpect(jsonPath("$.data.cinemaId").value(101));
    }

    @Test
    void reportingBindsDatesAndCinemaAndReturnsCapacity() throws Exception {
        LocalDate day = LocalDate.of(2026, 1, 15);
        when(showtimeService.getReportingShowtimes(day, day, 7L)).thenReturn(List.of(
                new ReportingShowtimeResponse(9L, 5L, "Movie", 7L, "Cinema", 3L, "Room",
                        day.atTime(10, 0), day.atTime(12, 0), 80)));

        mvc.perform(get("/internal/v1/catalog/showtimes/reporting")
                        .param("from", "2026-01-15").param("to", "2026-01-15").param("cinemaId", "7")
                        .header("X-Internal-Service-Secret", "internal-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].showtimeId").value(9))
                .andExpect(jsonPath("$.data[0].cinemaId").value(7))
                .andExpect(jsonPath("$.data[0].capacity").value(80));
    }

    @Test
    void nonexistentShowtimeReturnsNotFound() throws Exception {
        when(showtimeService.findById(404L)).thenThrow(new NotFoundException("Showtime not found"));

        mvc.perform(get("/internal/v1/catalog/showtimes/404/scope")
                        .header("X-Internal-Service-Secret", "internal-test"))
                .andExpect(status().isNotFound());
    }
}
