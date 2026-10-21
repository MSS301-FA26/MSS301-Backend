package com.cinemaai.booking.service;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.client.dto.CatalogQuoteDto;
import com.cinemaai.booking.dto.request.CheckoutBookingRequest;
import com.cinemaai.booking.dto.request.HoldSeatsRequest;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.ConflictException;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingProductItemRepository;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.BookingSeatRepository;
import com.cinemaai.booking.repository.BookingTicketRepository;
import com.cinemaai.booking.service.impl.BookingServiceImpl;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingExpirationLifecycleTest {
    private BookingRepository bookings;
    private BookingSeatRepository seats;
    private CatalogClient catalog;
    private PaymentClient payment;
    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        bookings = mock(BookingRepository.class);
        seats = mock(BookingSeatRepository.class);
        catalog = mock(CatalogClient.class);
        payment = mock(PaymentClient.class);
        service = new BookingServiceImpl(bookings, seats,
                mock(BookingTicketRepository.class), mock(BookingProductItemRepository.class),
                catalog, payment, mock(BookingMapper.class));
    }

    @Test
    void checkoutExpiresBookingAndOnlyReleasesHeldSeatsBeforeAnyQuote() {
        Booking booking = booking(BookingStatus.HOLDING, LocalDateTime.now().minusSeconds(1));
        BookingSeat held = seat(BookingSeatStatus.HOLDING);
        BookingSeat booked = seat(BookingSeatStatus.BOOKED);
        booking.setSeats(List.of(held, booked));
        when(bookings.findByIdAndUserIdForUpdate(7L, 1L)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class,
                () -> service.checkout(1L, 7L, new CheckoutBookingRequest(null, null, 0)));

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertEquals(BookingSeatStatus.RELEASED, held.getStatus());
        assertEquals(BookingSeatStatus.BOOKED, booked.getStatus());
        assertNotNull(booking.getCancelledAt());
        verify(bookings).save(booking);
        verifyNoInteractions(catalog);
    }

    @Test
    void markPaidRejectsExpiredBookingWithoutIssuingTickets() {
        Booking booking = booking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now().minusSeconds(1));
        BookingSeat held = seat(BookingSeatStatus.HOLDING);
        booking.setSeats(List.of(held));
        when(bookings.findByIdForUpdate(7L)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> service.markPaidInternal(7L, "TXN"));

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertEquals(BookingSeatStatus.RELEASED, held.getStatus());
        assertNull(held.getTicketCode());
        verify(bookings).save(booking);
    }

    @Test
    void markPaidDoesNotReviveCancelledBooking() {
        Booking booking = booking(BookingStatus.CANCELLED, LocalDateTime.now().plusMinutes(1));
        booking.setSeats(List.of(seat(BookingSeatStatus.RELEASED)));
        when(bookings.findByIdForUpdate(7L)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> service.markPaidInternal(7L, "TXN"));

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookings, never()).save(any());
    }

    @Test
    void directMarkPaidDoesNotAwardLoyaltyBeforeBookingCommit() {
        Booking booking = booking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(1));
        booking.setSeats(List.of(seat(BookingSeatStatus.HOLDING)));
        when(bookings.findByIdForUpdate(7L)).thenReturn(Optional.of(booking));

        service.markPaidInternal(7L, "TXN");

        assertEquals(BookingStatus.PAID, booking.getStatus());
        verify(bookings).save(booking);
        verifyNoInteractions(payment);
    }

    @Test
    void deadlineEqualityAndMissingDeadlineFailClosed() {
        LocalDateTime deadline = LocalDateTime.of(2026, 10, 21, 10, 0);
        Booking exactBoundary = booking(BookingStatus.HOLDING, deadline);
        Booking missingDeadline = booking(BookingStatus.HOLDING, null);

        assertEquals(Boolean.TRUE,
                ReflectionTestUtils.invokeMethod(service, "isHoldExpired", exactBoundary, deadline));
        assertEquals(Boolean.TRUE,
                ReflectionTestUtils.invokeMethod(service, "isHoldExpired", missingDeadline, deadline));
        assertEquals(Boolean.FALSE,
                ReflectionTestUtils.invokeMethod(service, "isHoldExpired", exactBoundary, deadline.minusNanos(1)));
    }

    @Test
    void expirationExceptionIsConfiguredToCommitCleanupInsteadOfRollingBack() throws Exception {
        for (String method : List.of("checkout", "updateHoldingItems", "cancelBooking", "markPaidInternal")) {
            var candidate = java.util.Arrays.stream(BookingServiceImpl.class.getMethods())
                    .filter(m -> m.getName().equals(method)).findFirst().orElseThrow();
            Transactional transactional = candidate.getAnnotation(Transactional.class);
            assertNotNull(transactional, method);
            assertTrue(List.of(transactional.noRollbackFor())
                    .contains(com.cinemaai.booking.exception.BookingExpiredException.class), method);
        }
    }

    @Test
    void replacementHoldLocksExistingBookingAndDoesNotReleaseSoldSeat() {
        Booking oldHold = booking(BookingStatus.HOLDING, LocalDateTime.now().plusMinutes(1));
        BookingSeat held = seat(BookingSeatStatus.HOLDING);
        BookingSeat booked = seat(BookingSeatStatus.BOOKED);
        oldHold.setSeats(List.of(held, booked));
        when(catalog.getQuote(any())).thenReturn(validQuote());
        when(bookings.findExpiredHoldsForSeatsForUpdate(anyLong(), anyList(), anyList(), any()))
                .thenReturn(List.of());
        when(bookings.findByUserIdAndShowtimeIdAndStatusInForUpdate(1L, 9L, List.of(BookingStatus.HOLDING)))
                .thenReturn(List.of(oldHold));
        when(seats.findConflictedSeatIds(eq(9L), anyList(), any())).thenReturn(List.of(11L));

        assertThrows(ConflictException.class, () -> service.holdSeats(1L,
                new HoldSeatsRequest(9L, List.of(11L), false, null, null, 0)));

        verify(bookings).findByUserIdAndShowtimeIdAndStatusInForUpdate(
                1L, 9L, List.of(BookingStatus.HOLDING));
        assertEquals(BookingStatus.CANCELLED, oldHold.getStatus());
        assertEquals(BookingSeatStatus.RELEASED, held.getStatus());
        assertEquals(BookingSeatStatus.BOOKED, booked.getStatus());
    }

    private Booking booking(BookingStatus status, LocalDateTime expiresAt) {
        return Booking.builder().id(7L).bookingCode("BK7").userId(1L).showtimeId(9L)
                .status(status).holdExpiresAt(expiresAt).loyaltyPointsRedeemed(0).build();
    }

    private BookingSeat seat(BookingSeatStatus status) {
        return BookingSeat.builder().seatId(11L).rowLabel("A").seatNumber(1).status(status).build();
    }

    private CatalogQuoteDto.Response validQuote() {
        return new CatalogQuoteDto.Response("Q", Instant.now().plusSeconds(60),
                new CatalogQuoteDto.Response.ShowtimeSnapshot(9L, 2L, "Movie", null,
                        "Cinema", "Room", LocalDateTime.now().plusHours(1), 10L),
                List.of(new CatalogQuoteDto.Response.SeatSnapshot(11L, "A1", "STANDARD", BigDecimal.TEN)),
                List.of(new CatalogQuoteDto.Response.TicketSnapshot(11L, "ADULT", 1,
                        BigDecimal.TEN, BigDecimal.TEN)),
                List.of(), BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
    }
}
