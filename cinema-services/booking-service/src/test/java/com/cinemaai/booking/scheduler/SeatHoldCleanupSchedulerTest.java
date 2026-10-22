package com.cinemaai.booking.scheduler;

import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.repository.BookingRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SeatHoldCleanupSchedulerTest {
    @Test
    void cleanupExpiresOnlyReturnedActiveBookingsAndNeverReleasesSoldSeats() {
        BookingRepository bookings = mock(BookingRepository.class);
        BookingSeat held = BookingSeat.builder().status(BookingSeatStatus.HOLDING).build();
        BookingSeat booked = BookingSeat.builder().status(BookingSeatStatus.BOOKED).build();
        BookingSeat checkedIn = BookingSeat.builder().status(BookingSeatStatus.CHECKED_IN).build();
        Booking expired = Booking.builder().status(BookingStatus.PENDING_PAYMENT)
                .seats(List.of(held, booked, checkedIn)).build();
        when(bookings.findExpiredActiveBookingsForUpdate(anyList(), any())).thenReturn(List.of(expired));

        new SeatHoldCleanupScheduler(bookings).cleanupExpiredHolds();

        assertEquals(BookingStatus.EXPIRED, expired.getStatus());
        assertEquals(BookingSeatStatus.RELEASED, held.getStatus());
        assertEquals(BookingSeatStatus.BOOKED, booked.getStatus());
        assertEquals(BookingSeatStatus.CHECKED_IN, checkedIn.getStatus());
        verify(bookings).saveAll(List.of(expired));
    }

    @Test
    void repeatedCleanupWithNoEligibleRowsHasNoSideEffects() {
        BookingRepository bookings = mock(BookingRepository.class);
        when(bookings.findExpiredActiveBookingsForUpdate(anyList(), any())).thenReturn(List.of());
        SeatHoldCleanupScheduler scheduler = new SeatHoldCleanupScheduler(bookings);

        scheduler.cleanupExpiredHolds();
        scheduler.cleanupExpiredHolds();

        verify(bookings, never()).saveAll(any());
    }
}
