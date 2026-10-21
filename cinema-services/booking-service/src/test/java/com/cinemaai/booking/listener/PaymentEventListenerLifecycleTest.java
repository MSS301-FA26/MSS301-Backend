package com.cinemaai.booking.listener;

import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.event.PaymentSucceededEvent;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.OrderRepository;
import com.cinemaai.booking.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentEventListenerLifecycleTest {
    private BookingRepository bookings;
    private ProcessedEventRepository processed;
    private OrderRepository orders;
    private PaymentClient payment;
    private ObjectMapper json;
    private PaymentEventListener listener;

    @BeforeEach
    void setUp() {
        bookings = mock(BookingRepository.class);
        processed = mock(ProcessedEventRepository.class);
        payment = mock(PaymentClient.class);
        json = new ObjectMapper().findAndRegisterModules();
        orders = mock(OrderRepository.class);
        listener = new PaymentEventListener(bookings, orders, processed, payment, json);
    }

    @Test
    void delayedEventDoesNotReviveExpiredBookingOrIssueTicket() throws Exception {
        BookingSeat released = BookingSeat.builder().rowLabel("A").seatNumber(1)
                .status(BookingSeatStatus.RELEASED).build();
        Booking booking = Booking.builder().id(7L).bookingCode("BK7").status(BookingStatus.EXPIRED)
                .seats(List.of(released)).build();
        when(bookings.findByIdForUpdate(7L)).thenReturn(Optional.of(booking));

        listener.onPaymentSucceeded(payload("evt-expired"));

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertEquals(BookingSeatStatus.RELEASED, released.getStatus());
        assertNull(released.getTicketCode());
        verify(bookings, never()).save(any());
        verifyNoInteractions(payment);
    }

    @Test
    void eventCannotReviveCancelledBooking() throws Exception {
        Booking booking = Booking.builder().id(7L).bookingCode("BK7").status(BookingStatus.CANCELLED)
                .seats(List.of(BookingSeat.builder().status(BookingSeatStatus.RELEASED).build())).build();
        when(bookings.findByIdForUpdate(7L)).thenReturn(Optional.of(booking));

        listener.onPaymentSucceeded(payload("evt-cancelled"));

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookings, never()).save(any());
        verifyNoInteractions(payment);
    }

    @Test
    void duplicateEventHasNoSideEffects() throws Exception {
        when(processed.existsById("evt-duplicate")).thenReturn(true);
        listener.onPaymentSucceeded(payload("evt-duplicate"));
        verifyNoInteractions(bookings, payment);
        verify(processed, never()).save(any());
    }

    @Test
    void validPendingPaymentEventBooksHeldSeatOnce() throws Exception {
        BookingSeat held = BookingSeat.builder().rowLabel("A").seatNumber(1)
                .status(BookingSeatStatus.HOLDING).build();
        Booking booking = Booking.builder().id(7L).bookingCode("BK7").userId(1L).cinemaId(2L)
                .status(BookingStatus.PENDING_PAYMENT).holdExpiresAt(LocalDateTime.now().plusMinutes(1))
                .loyaltyPointsRedeemed(0).totalAmount(BigDecimal.TEN).seats(List.of(held)).build();
        when(bookings.findByIdForUpdate(7L)).thenReturn(Optional.of(booking));

        listener.onPaymentSucceeded(payload("evt-valid"));

        assertEquals(BookingStatus.PAID, booking.getStatus());
        assertEquals(BookingSeatStatus.BOOKED, held.getStatus());
        assertEquals("BK7-A1", held.getTicketCode());
        verify(bookings).save(booking);
        verify(payment).awardLoyaltyPoints(1L, 7L, "BK7", BigDecimal.TEN, 0, 2L);
        verify(processed).save(any());
    }

    @Test
    void eventForAlreadyPaidBookingRetriesIdempotentLoyaltyAward() throws Exception {
        Booking booking = Booking.builder().id(7L).bookingCode("BK7").userId(1L).cinemaId(2L)
                .totalAmount(BigDecimal.TEN).loyaltyPointsRedeemed(0).status(BookingStatus.PAID)
                .seats(List.of(BookingSeat.builder().status(BookingSeatStatus.BOOKED).build())).build();
        when(bookings.findByIdForUpdate(7L)).thenReturn(Optional.of(booking));

        listener.onPaymentSucceeded(payload("evt-paid-retry"));

        verify(bookings, never()).save(any());
        verify(payment).awardLoyaltyPoints(1L, 7L, "BK7", BigDecimal.TEN, 0, 2L);
        verify(processed).save(any());
    }

    @Test
    void loyaltyFailureLeavesEventUnprocessedForRetry() throws Exception {
        Booking booking = Booking.builder().id(7L).bookingCode("BK7").userId(1L).cinemaId(2L)
                .totalAmount(BigDecimal.TEN).loyaltyPointsRedeemed(0).status(BookingStatus.PAID)
                .seats(List.of(BookingSeat.builder().status(BookingSeatStatus.BOOKED).build())).build();
        when(bookings.findByIdForUpdate(7L)).thenReturn(Optional.of(booking));
        doThrow(new IllegalStateException("loyalty unavailable")).when(payment)
                .awardLoyaltyPoints(1L, 7L, "BK7", BigDecimal.TEN, 0, 2L);

        assertThrows(IllegalStateException.class, () -> listener.onPaymentSucceeded(payload("evt-retry")));

        verify(processed, never()).save(any());
    }

    @Test
    void legacyAndCanonicalOrderEventsUseTheSameFulfillmentPath() throws Exception {
        com.cinemaai.booking.entity.Order legacyOrder = com.cinemaai.booking.entity.Order.builder()
                .id(8L).foodOrderCode("FO-8").status("PENDING_PAYMENT").build();
        com.cinemaai.booking.entity.Order canonicalOrder = com.cinemaai.booking.entity.Order.builder()
                .id(8L).foodOrderCode("FO-8").status("PENDING_PAYMENT").build();
        when(orders.findById(8L)).thenReturn(Optional.of(legacyOrder), Optional.of(canonicalOrder));

        listener.onPaymentSucceeded("""
                {"eventId":"legacy","paymentId":3,"foodOrderId":8,"userId":1,
                 "amount":10,"provider":"VNPAY","paidAt":"2026-01-01T00:00:00"}
                """);
        listener.onPaymentSucceeded("""
                {"eventId":"canonical","paymentId":3,"orderId":8,"userId":1,
                 "amount":10,"provider":"VNPAY","paidAt":"2026-01-01T00:00:00"}
                """);

        assertEquals("PAID", legacyOrder.getStatus());
        assertEquals("PAID", canonicalOrder.getStatus());
        verify(orders, times(2)).save(any());
        verify(processed, times(2)).save(any());
    }

    @Test
    void conflictingOrMalformedEventIsNotAcknowledgedAsProcessed() {
        String conflict = """
                {"eventId":"bad","paymentId":3,"foodOrderId":8,"orderId":9,"userId":1,
                 "amount":10,"provider":"VNPAY","paidAt":"2026-01-01T00:00:00"}
                """;

        assertThrows(IllegalArgumentException.class, () -> listener.onPaymentSucceeded(conflict));
        assertThrows(IllegalArgumentException.class, () -> listener.onPaymentSucceeded("not-json"));
        verify(processed, never()).save(any());
        verifyNoInteractions(orders);
    }

    private String payload(String eventId) throws Exception {
        return json.writeValueAsString(new PaymentSucceededEvent(eventId, 3L, 7L, null, 1L,
                BigDecimal.TEN, "VNPAY", LocalDateTime.now()));
    }
}
