package com.sba301.cinemaai.booking;

import com.sba301.cinemaai.entity.Room;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.SeatRow;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.repository.BookingSeatRepository;
import com.sba301.cinemaai.repository.SeatRepository;
import com.sba301.cinemaai.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SeatGapValidationTests {

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Room testRoom;
    private Showtime testShowtime;
    private SeatRow testRowA;

    @BeforeEach
    void setUp() {
        testRoom = mock(Room.class);
        lenient().when(testRoom.getId()).thenReturn(1L);

        testShowtime = mock(Showtime.class);
        lenient().when(testShowtime.getRoom()).thenReturn(testRoom);

        testRowA = mock(SeatRow.class);
        lenient().when(testRowA.getRowLabel()).thenReturn("A");
    }

    private Seat createSeat(Long id, String row, int seatNumber, int displayCol, SeatType type) {
        Seat seat = new Seat(testRoom, testRowA, seatNumber, displayCol, type);
        ReflectionTestUtils.setField(seat, "id", id);
        return seat;
    }

    private void invokeValidation(Showtime showtime, List<Seat> requestedSeats) throws Throwable {
        Method method = BookingServiceImpl.class.getDeclaredMethod("validateNoOrphanSeats", Showtime.class, List.class);
        method.setAccessible(true);
        try {
            method.invoke(bookingService, showtime, requestedSeats);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    void testXOX_shouldThrow() {
        // Row 3 seats: A1, A2, A3
        Seat s1 = createSeat(1L, "A", 1, 1, SeatType.SINGLE);
        Seat s2 = createSeat(2L, "A", 2, 2, SeatType.SINGLE);
        Seat s3 = createSeat(3L, "A", 3, 3, SeatType.SINGLE);

        when(bookingSeatRepository.findByShowtime(any())).thenReturn(List.of());
        when(seatRepository.findByRoomId(1L)).thenReturn(List.of(s1, s2, s3));

        // User picks A1 and A3, leaving A2 empty
        assertThatThrownBy(() -> invokeValidation(testShowtime, List.of(s1, s3)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("INVALID_SEAT_GAP");
    }

    @Test
    void testXOOX_shouldPass() throws Throwable {
        // Row 4 seats: A1, A2, A3, A4
        Seat s1 = createSeat(1L, "A", 1, 1, SeatType.SINGLE);
        Seat s2 = createSeat(2L, "A", 2, 2, SeatType.SINGLE);
        Seat s3 = createSeat(3L, "A", 3, 3, SeatType.SINGLE);
        Seat s4 = createSeat(4L, "A", 4, 4, SeatType.SINGLE);

        when(bookingSeatRepository.findByShowtime(any())).thenReturn(List.of());
        when(seatRepository.findByRoomId(1L)).thenReturn(List.of(s1, s2, s3, s4));

        // User picks A1 and A4, leaving 2 seats A2, A3 empty
        invokeValidation(testShowtime, List.of(s1, s4));
    }

    @Test
    void testEndEdge_OXXX_shouldPass() throws Throwable {
        Seat s1 = createSeat(1L, "A", 1, 1, SeatType.SINGLE);
        Seat s2 = createSeat(2L, "A", 2, 2, SeatType.SINGLE);
        Seat s3 = createSeat(3L, "A", 3, 3, SeatType.SINGLE);
        Seat s4 = createSeat(4L, "A", 4, 4, SeatType.SINGLE);

        when(bookingSeatRepository.findByShowtime(any())).thenReturn(List.of());
        when(seatRepository.findByRoomId(1L)).thenReturn(List.of(s1, s2, s3, s4));

        // A1 left free at row edge
        invokeValidation(testShowtime, List.of(s2, s3, s4));
    }

    @Test
    void testEndEdge_XXXO_shouldPass() throws Throwable {
        Seat s1 = createSeat(1L, "A", 1, 1, SeatType.SINGLE);
        Seat s2 = createSeat(2L, "A", 2, 2, SeatType.SINGLE);
        Seat s3 = createSeat(3L, "A", 3, 3, SeatType.SINGLE);
        Seat s4 = createSeat(4L, "A", 4, 4, SeatType.SINGLE);

        when(bookingSeatRepository.findByShowtime(any())).thenReturn(List.of());
        when(seatRepository.findByRoomId(1L)).thenReturn(List.of(s1, s2, s3, s4));

        // A4 left free at row edge
        invokeValidation(testShowtime, List.of(s1, s2, s3));
    }

    @Test
    void testAisleSegment_shouldNotCheckAcrossAisle() throws Throwable {
        // A1, A2, A3 (displayCol 1, 2, 3) | Aisle | A4, A5, A6 (displayCol 5, 6, 7)
        Seat s1 = createSeat(1L, "A", 1, 1, SeatType.SINGLE);
        Seat s2 = createSeat(2L, "A", 2, 2, SeatType.SINGLE);
        Seat s3 = createSeat(3L, "A", 3, 3, SeatType.SINGLE);
        Seat s4 = createSeat(4L, "A", 4, 5, SeatType.SINGLE);
        Seat s5 = createSeat(5L, "A", 5, 6, SeatType.SINGLE);
        Seat s6 = createSeat(6L, "A", 6, 7, SeatType.SINGLE);

        when(bookingSeatRepository.findByShowtime(any())).thenReturn(List.of());
        when(seatRepository.findByRoomId(1L)).thenReturn(List.of(s1, s2, s3, s4, s5, s6));

        // User picks A3 and A4 across aisle
        invokeValidation(testShowtime, List.of(s3, s4));
    }
}
