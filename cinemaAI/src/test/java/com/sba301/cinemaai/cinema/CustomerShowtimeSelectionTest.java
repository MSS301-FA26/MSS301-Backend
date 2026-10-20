package com.sba301.cinemaai.cinema;

import com.sba301.cinemaai.dto.response.cinema.CustomerShowtimeSlotResponse;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.Room;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.enums.MovieApprovalStatus;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.enums.RoomStatus;
import com.sba301.cinemaai.enums.RoomType;
import com.sba301.cinemaai.enums.SeatStatus;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.BookingSeatRepository;
import com.sba301.cinemaai.repository.MovieGenreRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.SeatRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import com.sba301.cinemaai.service.impl.ShowtimeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerShowtimeSelectionTest {

    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingSeatRepository bookingSeatRepository;
    @Mock
    private MovieGenreRepository movieGenreRepository;

    @InjectMocks
    private ShowtimeServiceImpl showtimeService;

    private Movie approvedMovie;
    private Room roomA;
    private Room roomC;

    @BeforeEach
    void setUp() {
        approvedMovie = org.mockito.Mockito.mock(Movie.class);
        when(approvedMovie.getId()).thenReturn(1L);
        when(approvedMovie.getTitle()).thenReturn("Test Movie");
        when(approvedMovie.getStatus()).thenReturn(MovieStatus.NOW_SHOWING);
        when(approvedMovie.getApprovalStatus()).thenReturn(MovieApprovalStatus.APPROVED);

        roomA = org.mockito.Mockito.mock(Room.class);
        when(roomA.getId()).thenReturn(101L);
        when(roomA.getStatus()).thenReturn(RoomStatus.ACTIVE);
        when(roomA.getRoomType()).thenReturn(RoomType.STANDARD);

        roomC = org.mockito.Mockito.mock(Room.class);
        when(roomC.getId()).thenReturn(102L);
        when(roomC.getStatus()).thenReturn(RoomStatus.ACTIVE);
        when(roomC.getRoomType()).thenReturn(RoomType.STANDARD);
    }

    private Showtime createShowtime(Long id, Room room, LocalDateTime startTime, BigDecimal price) {
        Showtime st = mock(Showtime.class);
        when(st.getId()).thenReturn(id);
        when(st.getMovie()).thenReturn(approvedMovie);
        when(st.getRoom()).thenReturn(room);
        when(st.getStartTime()).thenReturn(startTime);
        when(st.getEndTime()).thenReturn(startTime.plusHours(2));
        when(st.getBasePrice()).thenReturn(price);
        when(st.getStatus()).thenReturn(ShowtimeStatus.OPEN);
        return st;
    }

    private Seat createMockSeat(Room room, Long id) {
        Seat seat = mock(Seat.class);
        when(seat.getId()).thenReturn(id);
        when(seat.getRoom()).thenReturn(room);
        return seat;
    }

    @Test
    @DisplayName("CASE 1 & CASE 5: Slots are grouped, sorted by startTime asc, and roomName is hidden")
    void shouldReturnAvailableSlotsGroupedAndSorted() {
        LocalDateTime time930 = LocalDateTime.now().plusDays(1).withHour(9).withMinute(30).withSecond(0).withNano(0);
        LocalDateTime time1300 = LocalDateTime.now().plusDays(1).withHour(13).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime time2030 = LocalDateTime.now().plusDays(1).withHour(20).withMinute(30).withSecond(0).withNano(0);

        Showtime st930 = createShowtime(1L, roomC, time930, BigDecimal.valueOf(90000));
        Showtime st1300A = createShowtime(2L, roomA, time1300, BigDecimal.valueOf(90000));
        Showtime st1300C = createShowtime(3L, roomC, time1300, BigDecimal.valueOf(90000));
        Showtime st2030 = createShowtime(4L, roomC, time2030, BigDecimal.valueOf(90000));

        when(showtimeRepository.findCustomerCandidateShowtimes(eq(1L), any(), any()))
                .thenReturn(List.of(st930, st1300A, st1300C, st2030));

        Seat seatA1 = createMockSeat(roomA, 1L);
        Seat seatA2 = createMockSeat(roomA, 2L);
        Seat seatC1 = createMockSeat(roomC, 3L);
        Seat seatC2 = createMockSeat(roomC, 4L);
        Seat seatC3 = createMockSeat(roomC, 5L);

        when(seatRepository.findByRoomInAndStatus(anyCollection(), eq(SeatStatus.AVAILABLE)))
                .thenReturn(List.of(seatA1, seatA2, seatC1, seatC2, seatC3));
        when(bookingSeatRepository.findByShowtimeIn(anyCollection())).thenReturn(Collections.emptyList());

        List<CustomerShowtimeSlotResponse> slots = showtimeService.getCustomerAvailableSlots(1L, time930.toLocalDate());

        // There should be 3 slots: 09:30, 13:00 (grouped), 20:30
        assertEquals(3, slots.size());
        assertEquals(time930, slots.get(0).startTime());
        assertEquals(time1300, slots.get(1).startTime());
        assertEquals(time2030, slots.get(2).startTime());

        // For 13:00, Room C has 3 seats, Room A has 2 seats => Room C (id 3) should be chosen!
        assertEquals(3L, slots.get(1).showtimeId());
        assertEquals(3, slots.get(1).availableSeats());
    }

    @Test
    @DisplayName("CASE 2 & CASE 3: Only showtimes with availableSeats > 0 appear; sold-out slots are completely hidden")
    void shouldHideSoldOutShowtimes() {
        LocalDateTime time1300 = LocalDateTime.now().plusDays(1).withHour(13).withMinute(0).withSecond(0).withNano(0);

        Showtime st1300A = createShowtime(2L, roomA, time1300, BigDecimal.valueOf(90000));
        Showtime st1300C = createShowtime(3L, roomC, time1300, BigDecimal.valueOf(90000));

        when(showtimeRepository.findCustomerCandidateShowtimes(eq(1L), any(), any()))
                .thenReturn(List.of(st1300A, st1300C));

        // When Room A has 0 seats and Room C has 0 seats:
        when(seatRepository.findByRoomInAndStatus(anyCollection(), eq(SeatStatus.AVAILABLE)))
                .thenReturn(Collections.emptyList());
        when(bookingSeatRepository.findByShowtimeIn(anyCollection())).thenReturn(Collections.emptyList());

        List<CustomerShowtimeSlotResponse> slots = showtimeService.getCustomerAvailableSlots(1L, time1300.toLocalDate());

        // CASE 3: 0 seats in all rooms => 13:00 is NOT displayed!
        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("CASE 4: Shows cutoff: if candidates list is empty due to cutoff, returns empty list")
    void shouldExcludeExpiredShowtimes() {
        when(showtimeRepository.findCustomerCandidateShowtimes(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());

        List<CustomerShowtimeSlotResponse> slots = showtimeService.getCustomerAvailableSlots(1L, LocalDate.now());
        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("CASE 8 & 10: resolveCustomerShowtime rechecks and resolves to alternative if original sold out")
    void shouldResolveAlternativeWhenOriginalSoldOut() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(3);
        Showtime st1 = createShowtime(10L, roomA, futureTime, BigDecimal.valueOf(90000));
        Showtime st2 = createShowtime(20L, roomC, futureTime, BigDecimal.valueOf(90000));

        when(showtimeRepository.findWithDetailsById(10L)).thenReturn(Optional.of(st1));

        // Room A has 0 seats available
        // Room C has 5 seats available
        Seat seatC = createMockSeat(roomC, 100L);
        when(seatRepository.findByRoomInAndStatus(anyCollection(), eq(SeatStatus.AVAILABLE)))
                .thenReturn(List.of(seatC));
        when(bookingSeatRepository.findByShowtimeIn(anyCollection())).thenReturn(Collections.emptyList());

        when(showtimeRepository.findEquivalentCandidateShowtimes(eq(1L), eq(futureTime)))
                .thenReturn(List.of(st1, st2));

        CustomerShowtimeSlotResponse resolved = showtimeService.resolveCustomerShowtime(10L);

        assertNotNull(resolved);
        // Resolved to st2 (Room C) which has 1 seat available
        assertEquals(20L, resolved.showtimeId());
    }

    @Test
    @DisplayName("CASE 10: resolveCustomerShowtime throws BadRequestException when all are sold out")
    void shouldThrowWhenAllSoldOut() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(3);
        Showtime st1 = createShowtime(10L, roomA, futureTime, BigDecimal.valueOf(90000));

        when(showtimeRepository.findWithDetailsById(10L)).thenReturn(Optional.of(st1));
        when(seatRepository.findByRoomInAndStatus(anyCollection(), eq(SeatStatus.AVAILABLE)))
                .thenReturn(Collections.emptyList());
        when(bookingSeatRepository.findByShowtimeIn(anyCollection())).thenReturn(Collections.emptyList());
        when(showtimeRepository.findEquivalentCandidateShowtimes(eq(1L), eq(futureTime)))
                .thenReturn(List.of(st1));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            showtimeService.resolveCustomerShowtime(10L);
        });

        assertEquals("Khung giờ này vừa hết chỗ. Vui lòng chọn khung giờ khác.", ex.getMessage());
    }
}
