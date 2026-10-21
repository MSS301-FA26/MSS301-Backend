package com.cinemaai.booking.service;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.client.dto.CatalogQuoteDto;
import com.cinemaai.booking.dto.request.HoldSeatsRequest;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingFoodItemRepository;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.BookingSeatRepository;
import com.cinemaai.booking.repository.BookingTicketRepository;
import com.cinemaai.booking.service.impl.BookingServiceImpl;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.LongStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void expandedSeatPayloadIsFullyRequotedByCatalog() {
        List<Long> seatIds = LongStream.rangeClosed(1, 20).boxed().toList();
        when(catalog.getQuote(any())).thenThrow(new BadRequestException("quote marker"));

        assertThrows(BadRequestException.class, () -> service.holdSeats(1L,
                new HoldSeatsRequest(9L, seatIds, false, null, null, 0)));

        var requestCaptor = ArgumentCaptor.forClass(CatalogQuoteDto.Request.class);
        verify(catalog).getQuote(requestCaptor.capture());
        assertEquals(seatIds, requestCaptor.getValue().seatIds());
        assertEquals(20, requestCaptor.getValue().tickets().size());
        assertEquals(20, requestCaptor.getValue().tickets().stream().mapToInt(CatalogQuoteDto.Request.Ticket::quantity).sum());
        verifyNoInteractions(bookings, seats, payment);
    }

    @Test
    void rejectsSeatQuantityTamperingBeforeCatalogOrPersistence() {
        var forgedQuantity = new HoldSeatsRequest.TicketSelection(null, "ADULT", 20, 20);
        var hugeQuantity = new HoldSeatsRequest.TicketSelection(null, "ADULT", 20, Integer.MAX_VALUE);

        assertThrows(BadRequestException.class, () -> service.holdSeats(1L,
                new HoldSeatsRequest(9L, List.of(1L, 2L), false, List.of(forgedQuantity), null, 0)));
        assertThrows(BadRequestException.class, () -> service.holdSeats(1L,
                new HoldSeatsRequest(9L, List.of(1L, 2L), false, List.of(hugeQuantity), null, 0)));

        verifyNoInteractions(bookings, seats, catalog, payment);
    }

    @Test
    void rejectsCatalogQuoteThatDuplicatesOneSeatAndOmitsAnother() {
        BigDecimal price = BigDecimal.valueOf(90_000);
        var quote = new CatalogQuoteDto.Response(
                "q-1", Instant.now().plusSeconds(60),
                new CatalogQuoteDto.Response.ShowtimeSnapshot(9L, 3L, "Movie", null,
                        "Cinema", "Room", LocalDateTime.now().plusHours(2), 4L),
                List.of(
                        new CatalogQuoteDto.Response.SeatSnapshot(1L, "A01", "STANDARD", price),
                        new CatalogQuoteDto.Response.SeatSnapshot(2L, "A02", "STANDARD", price)),
                List.of(
                        new CatalogQuoteDto.Response.TicketSnapshot(1L, "ADULT", 1, price, price),
                        new CatalogQuoteDto.Response.TicketSnapshot(1L, "ADULT", 1, price, price)),
                List.of(), price.multiply(BigDecimal.valueOf(2)), BigDecimal.ZERO,
                price.multiply(BigDecimal.valueOf(2)));
        when(catalog.getQuote(any())).thenReturn(quote);

        assertThrows(BadRequestException.class, () -> service.holdSeats(1L,
                new HoldSeatsRequest(9L, List.of(1L, 2L), false, null, null, 0)));

        verifyNoInteractions(bookings, seats, payment);
    }
}
