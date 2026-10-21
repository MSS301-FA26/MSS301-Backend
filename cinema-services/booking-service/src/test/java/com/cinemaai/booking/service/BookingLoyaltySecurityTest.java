package com.cinemaai.booking.service;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.client.dto.CatalogQuoteDto;
import com.cinemaai.booking.dto.request.CheckoutBookingRequest;
import com.cinemaai.booking.dto.request.HoldSeatsRequest;
import com.cinemaai.booking.dto.request.UpdateHoldingBookingRequest;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingFoodItemRepository;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.BookingSeatRepository;
import com.cinemaai.booking.repository.BookingTicketRepository;
import com.cinemaai.booking.service.impl.BookingServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingLoyaltySecurityTest {
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
        service = new BookingServiceImpl(bookings, seats, mock(BookingTicketRepository.class),
                mock(BookingFoodItemRepository.class), catalog, payment, mock(BookingMapper.class));
    }

    @Test
    void positivePointsAreDeniedBeforeNewHoldMutation() {
        var request = new HoldSeatsRequest(9L, List.of(1L), false, null, null, 1_000_000);
        assertThrows(BadRequestException.class, () -> service.holdSeats(7L, request));
        verifyNoInteractions(bookings, seats, catalog, payment);
    }

    @Test
    void nullAndZeroRemainValidForNewHoldPolicy() {
        when(catalog.getQuote(any())).thenThrow(new BadRequestException("quote marker"));
        assertThrows(BadRequestException.class, () -> service.holdSeats(7L,
                new HoldSeatsRequest(9L, List.of(1L), false, null, null, null)));
        assertThrows(BadRequestException.class, () -> service.holdSeats(7L,
                new HoldSeatsRequest(9L, List.of(1L), false, null, null, 0)));
        verify(catalog, times(2)).getQuote(any());
        verifyNoInteractions(bookings, seats, payment);
    }

    @Test
    void positivePointsAreDeniedAtCheckoutAndUpdate() {
        Booking booking = activeBooking(0);
        when(bookings.findByIdAndUserIdForUpdate(99L, 7L)).thenReturn(Optional.of(booking));
        assertThrows(BadRequestException.class, () -> service.checkout(7L, 99L,
                new CheckoutBookingRequest(null, null, 1)));
        assertThrows(BadRequestException.class, () -> service.updateHoldingItems(7L, 99L,
                new UpdateHoldingBookingRequest(null, null, 1)));
        verify(bookings, never()).save(any());
        verifyNoInteractions(catalog, payment);
    }

    @Test
    void legacyBookingCannotBypassWithNullAndFailedZeroRecoveryDoesNotMutate() {
        Booking booking = activeBooking(500);
        when(bookings.findByIdAndUserIdForUpdate(99L, 7L)).thenReturn(Optional.of(booking));
        when(payment.hasActiveBookingPayment(99L)).thenReturn(false);
        when(catalog.getQuote(any())).thenThrow(new BadRequestException("re-quote failed"));

        assertThrows(BadRequestException.class, () -> service.checkout(7L, 99L,
                new CheckoutBookingRequest(null, null, null)));
        assertThrows(BadRequestException.class, () -> service.checkout(7L, 99L,
                new CheckoutBookingRequest(null, null, 0)));

        verify(bookings, never()).save(any());
        verify(catalog).getQuote(any());
        org.junit.jupiter.api.Assertions.assertEquals(500, booking.getLoyaltyPointsRedeemed());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("500"), booking.getDiscountAmount());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("99500"), booking.getTotalAmount());
    }

    @Test
    void internalMarkPaidRejectsUnpaidLegacyDiscount() {
        Booking booking = activeBooking(500);
        when(bookings.findByIdForUpdate(99L)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> service.markPaidInternal(99L, "TXN"));

        verify(bookings, never()).save(any());
        verifyNoInteractions(payment);
        org.junit.jupiter.api.Assertions.assertEquals(BookingStatus.HOLDING, booking.getStatus());
    }

    @Test
    void explicitZeroRecoversLegacyBookingThroughAuthoritativeRequote() {
        Booking booking = activeBooking(500);
        when(bookings.findByIdAndUserIdForUpdate(99L, 7L)).thenReturn(Optional.of(booking));
        when(payment.hasActiveBookingPayment(99L)).thenReturn(false);
        when(catalog.getQuote(any())).thenReturn(validQuote());
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.checkout(7L, 99L, new CheckoutBookingRequest(null, null, 0));

        org.junit.jupiter.api.Assertions.assertEquals(0, booking.getLoyaltyPointsRedeemed());
        org.junit.jupiter.api.Assertions.assertEquals(BigDecimal.ZERO, booking.getDiscountAmount());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("100000"), booking.getTotalAmount());
        org.junit.jupiter.api.Assertions.assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
        verify(bookings).save(booking);
    }

    @Test
    void activePaymentPreventsLegacyRepriceBeforeCatalogOrMutation() {
        Booking booking = activeBooking(500);
        when(bookings.findByIdAndUserIdForUpdate(99L, 7L)).thenReturn(Optional.of(booking));
        when(payment.hasActiveBookingPayment(99L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.checkout(7L, 99L,
                new CheckoutBookingRequest(null, null, 0)));

        verifyNoInteractions(catalog);
        verify(bookings, never()).save(any());
        org.junit.jupiter.api.Assertions.assertEquals(500, booking.getLoyaltyPointsRedeemed());
    }

    private Booking activeBooking(int redeemed) {
        Booking booking = Booking.builder()
                .id(99L).userId(7L).showtimeId(9L).cinemaId(10L)
                .status(BookingStatus.HOLDING)
                .holdExpiresAt(LocalDateTime.now().plusMinutes(2))
                .subtotal(new BigDecimal("100000"))
                .discountAmount(BigDecimal.valueOf(redeemed))
                .loyaltyPointsRedeemed(redeemed)
                .totalAmount(BigDecimal.valueOf(100000L - redeemed))
                .build();
        booking.setSeats(List.of(BookingSeat.builder().seatId(1L).booking(booking).build()));
        return booking;
    }

    private CatalogQuoteDto.Response validQuote() {
        return new CatalogQuoteDto.Response("Q", Instant.now().plusSeconds(60),
                new CatalogQuoteDto.Response.ShowtimeSnapshot(9L, 2L, "Movie", null,
                        "Cinema", "Room", LocalDateTime.now().plusHours(1), 10L),
                List.of(new CatalogQuoteDto.Response.SeatSnapshot(1L, "A1", "STANDARD", new BigDecimal("100000"))),
                List.of(new CatalogQuoteDto.Response.TicketSnapshot(1L, "ADULT", 1,
                        new BigDecimal("100000"), new BigDecimal("100000"))),
                List.of(), new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"));
    }
}
