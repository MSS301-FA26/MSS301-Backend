package com.cinemaai.catalog.service;

import com.cinemaai.catalog.entity.*;
import com.cinemaai.catalog.enums.SeatStatus;
import com.cinemaai.catalog.enums.ShowtimeStatus;
import com.cinemaai.catalog.repository.SeatRepository;
import com.cinemaai.catalog.repository.ShowtimeRepository;
import com.cinemaai.catalog.service.impl.ShowtimeServiceImpl;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportingShowtimesTest {
    @Mock ShowtimeRepository showtimes;
    @Mock SeatRepository seats;
    @InjectMocks ShowtimeServiceImpl service;

    @Test void usesRealAvailableSeatsAndIncludesShowsWithNoSalesButExcludesCancelled() {
        LocalDate day = LocalDate.of(2026, 1, 1);
        Showtime show = mock(Showtime.class), cancelled = mock(Showtime.class);
        Room room = mock(Room.class); Cinema cinema = mock(Cinema.class); Movie movie = mock(Movie.class);
        Seat a = mock(Seat.class), b = mock(Seat.class);
        when(showtimes.searchAdmin(null, null, 7L, null, day.atStartOfDay(), day.plusDays(1).atStartOfDay(), Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(show, cancelled)));
        when(show.getStatus()).thenReturn(ShowtimeStatus.OPEN); when(cancelled.getStatus()).thenReturn(ShowtimeStatus.CANCELLED);
        when(show.getRoom()).thenReturn(room); when(room.getId()).thenReturn(3L); when(room.getName()).thenReturn("Phòng 3");
        when(room.getCinema()).thenReturn(cinema); when(cinema.getId()).thenReturn(7L); when(cinema.getName()).thenReturn("Rạp A");
        when(show.getMovie()).thenReturn(movie); when(movie.getId()).thenReturn(1L); when(movie.getTitle()).thenReturn("Phim");
        when(show.getId()).thenReturn(9L); when(show.getStartTime()).thenReturn(day.atTime(9, 0)); when(show.getEndTime()).thenReturn(day.atTime(11, 0));
        when(seats.findByRoomInAndStatus(List.of(room), SeatStatus.AVAILABLE)).thenReturn(List.of(a, b));
        when(a.getRoom()).thenReturn(room); when(b.getRoom()).thenReturn(room);
        var result = service.getReportingShowtimes(day, day, 7L);
        assertEquals(1, result.size()); assertEquals(2, result.getFirst().capacity()); assertEquals(9L, result.getFirst().showtimeId());
        verify(seats).findByRoomInAndStatus(List.of(room), SeatStatus.AVAILABLE);
    }
}
