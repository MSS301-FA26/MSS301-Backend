package com.cinemaai.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.cinemaai.catalog.dto.request.quote.CheckoutQuoteRequest;
import com.cinemaai.catalog.entity.*;
import com.cinemaai.catalog.enums.*;
import com.cinemaai.catalog.exception.BadRequestException;
import com.cinemaai.catalog.repository.*;
import com.cinemaai.catalog.service.impl.CheckoutQuoteServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CheckoutQuoteServiceTest {
    @Mock ShowtimeRepository showtimes;
    @Mock SeatRepository seats;
    @Mock FoodItemRepository items;
    @Mock FoodComboRepository combos;
    private CheckoutQuoteServiceImpl service;
    private Showtime showtime;
    private Seat standardSeat;

    @BeforeEach
    void setUp() {
        service = new CheckoutQuoteServiceImpl(showtimes, seats, items, combos);
        ReflectionTestUtils.setField(service, "ttlSeconds", 300L);

        Cinema cinema = withId(new Cinema("CinemaAI Central", "Address", "City", "0123"), 1L);
        Room room = withId(new Room(cinema, "Hall 1", RoomType.STANDARD, 2, 4), 10L);
        SeatRow row = withId(new SeatRow(room, "A", 1, 1, SeatType.STANDARD), 20L);
        standardSeat = withId(new Seat(room, row, 1, 1, SeatType.STANDARD), 100L);
        Movie movie = withId(new Movie("Movie", 100, MovieStatus.NOW_SHOWING), 30L);
        movie.setAgeRating(AgeRating.P);
        LocalDateTime monday = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);
        while (monday.getDayOfWeek() != java.time.DayOfWeek.MONDAY) monday = monday.plusDays(1);
        showtime = withId(new Showtime(movie, room, monday, monday.plusHours(2), new BigDecimal("90000")), 40L);
        showtime.setAdultStandardPrice(new BigDecimal("90000"));
        showtime.setStatus(ShowtimeStatus.OPEN);
        when(showtimes.findWithDetailsById(40L)).thenReturn(Optional.of(showtime));
    }

    @Test
    void returnsAuthoritativeTicketAndFoodTotals() {
        FoodItem popcorn = withId(new FoodItem("Popcorn", "Large", new BigDecimal("45000")), 50L);
        when(seats.findAllById(any())).thenReturn(List.of(standardSeat));
        when(items.findById(50L)).thenReturn(Optional.of(popcorn));

        var response = service.quote(new CheckoutQuoteRequest(
                40L, List.of(100L),
                List.of(new CheckoutQuoteRequest.Ticket(100L, TicketType.ADULT, 22, 1)),
                List.of(new CheckoutQuoteRequest.Food(50L, false, 1))));

        assertThat(response.ticketSubtotal()).isEqualByComparingTo("90000");
        assertThat(response.foodSubtotal()).isEqualByComparingTo("45000");
        assertThat(response.subtotal()).isEqualByComparingTo("135000");
        assertThat(response.seats()).singleElement().satisfies(seat -> {
            assertThat(seat.seatLabel()).isEqualTo("A01");
            assertThat(seat.unitPrice()).isEqualByComparingTo("90000");
        });
    }

    @Test
    void rejectsSeatFromAnotherRoom() {
        Cinema cinema = withId(new Cinema("Other", "Address", "City", "0123"), 2L);
        Room otherRoom = withId(new Room(cinema, "Hall 2", RoomType.STANDARD, 1, 1), 11L);
        SeatRow otherRow = withId(new SeatRow(otherRoom, "A", 1, 1, SeatType.STANDARD), 21L);
        Seat wrongSeat = withId(new Seat(otherRoom, otherRow, 1, 1, SeatType.STANDARD), 101L);
        when(seats.findAllById(any())).thenReturn(List.of(wrongSeat));

        var request = new CheckoutQuoteRequest(40L, List.of(101L),
                List.of(new CheckoutQuoteRequest.Ticket(101L, TicketType.ADULT, 22, 1)), List.of());

        assertThatThrownBy(() -> service.quote(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("another room");
    }

    @Test
    void appliesWeekendSurchargeFromShowtimeDate() {
        LocalDateTime saturday = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);
        while (saturday.getDayOfWeek() != java.time.DayOfWeek.SATURDAY) saturday = saturday.plusDays(1);
        showtime.setStartTime(saturday);
        showtime.setEndTime(saturday.plusHours(2));
        when(seats.findAllById(any())).thenReturn(List.of(standardSeat));

        var response = service.quote(new CheckoutQuoteRequest(40L, List.of(100L),
                List.of(new CheckoutQuoteRequest.Ticket(100L, TicketType.ADULT, 22, 1)), List.of()));

        assertThat(response.ticketSubtotal()).isEqualByComparingTo("100000");
    }

    @Test
    void handlesFrontendPayloadWithoutTicketsAndWithFoodItemId() {
        FoodItem popcorn = withId(new FoodItem("Popcorn", "Large", new BigDecimal("45000")), 50L);
        when(seats.findAllById(any())).thenReturn(List.of(standardSeat));
        when(items.findById(50L)).thenReturn(Optional.of(popcorn));

        // Frontend sends seatIds, foods with foodItemId (no tickets, no productId, no isCombo)
        var food = new CheckoutQuoteRequest.Food(null, null, 2, 50L, null);
        var request = new CheckoutQuoteRequest(40L, List.of(100L), null, List.of(food), null, null, null);

        var response = service.quote(request);

        assertThat(response.ticketSubtotal()).isEqualByComparingTo("90000");
        assertThat(response.foodSubtotal()).isEqualByComparingTo("90000");
        assertThat(response.subtotal()).isEqualByComparingTo("180000");
        assertThat(response.total()).isEqualByComparingTo("180000");
        assertThat(response.tickets()).hasSize(1);
        assertThat(response.tickets().get(0).ticketType()).isEqualTo(TicketType.ADULT);
    }

    @Test
    void rejectsTicketQuantityThatDoesNotMatchSeatCount() {
        when(seats.findAllById(any())).thenReturn(List.of(standardSeat));
        var request = new CheckoutQuoteRequest(40L, List.of(100L),
                List.of(new CheckoutQuoteRequest.Ticket(null, TicketType.ADULT, 30, 0)), List.of());

        assertThatThrownBy(() -> service.quote(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Số lượng vé phải bằng số lượng ghế đã chọn.");
    }

    @Test
    void rejectsNegativeTicketQuantity() {
        when(seats.findAllById(any())).thenReturn(List.of(standardSeat));
        var request = new CheckoutQuoteRequest(40L, List.of(100L),
                List.of(new CheckoutQuoteRequest.Ticket(null, TicketType.ADULT, 30, -1)), List.of());

        assertThatThrownBy(() -> service.quote(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Số lượng vé không được âm");
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
