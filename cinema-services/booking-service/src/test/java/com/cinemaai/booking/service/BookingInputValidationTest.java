package com.cinemaai.booking.service;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.dto.request.HoldSeatsRequest;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingFoodItemRepository;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.BookingSeatRepository;
import com.cinemaai.booking.repository.BookingTicketRepository;
import com.cinemaai.booking.service.impl.BookingServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BookingInputValidationTest {
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
    void rejectsMalformedSeatsBeforeRepositoryMutation() {
        assertThrows(BadRequestException.class, () -> service.holdSeats(1L,
                new HoldSeatsRequest(9L, List.of(), false, null, null, 0)));
        assertThrows(BadRequestException.class, () -> service.holdSeats(1L,
                new HoldSeatsRequest(9L, List.of(1L, 1L), false, null, null, 0)));
        assertThrows(BadRequestException.class, () -> service.holdSeats(1L,
                new HoldSeatsRequest(9L, List.of(0L), false, null, null, 0)));
        verifyNoInteractions(bookings, seats, catalog, payment);
    }

    @Test
    void rejectsInvalidTicketAndLoyaltyBeforeSideEffects() {
        var invalidTicket = new HoldSeatsRequest.TicketSelection(1L, "NOT_A_TYPE", -1, 0);
        assertThrows(BadRequestException.class, () -> service.holdSeats(1L,
                new HoldSeatsRequest(9L, List.of(1L), false, List.of(invalidTicket), null, 0)));
        assertThrows(BadRequestException.class, () -> service.holdSeats(1L,
                new HoldSeatsRequest(9L, List.of(1L), false, null, null, -1)));
        verifyNoInteractions(bookings, seats, catalog, payment);
    }

    @Test
    void rejectsConflictingFoodIdentifiersBeforeSideEffects() {
        var food = new HoldSeatsRequest.FoodSelection(1L, false, 1, 2L, null);
        assertThrows(BadRequestException.class, () -> service.holdSeats(1L,
                new HoldSeatsRequest(9L, List.of(1L), false, null, List.of(food), 0)));
        verifyNoInteractions(bookings, seats, catalog, payment);
    }

    @Test
    void catalogValidationFailureOccursBeforeHoldMutation() {
        var request = new HoldSeatsRequest(9L, List.of(1L), false, null, null, 0);
        when(catalog.getQuote(any())).thenThrow(new BadRequestException("Seat belongs to another room"));

        assertThrows(BadRequestException.class, () -> service.holdSeats(1L, request));

        verifyNoInteractions(bookings, seats, payment);
    }
}
