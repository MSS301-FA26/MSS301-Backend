package com.cinemaai.booking.service;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.dto.ReportingShowtimeDto;
import com.cinemaai.booking.entity.*;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.OrderRepository;
import com.cinemaai.booking.service.impl.AdminAnalyticsServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceTest {
    @Mock BookingRepository bookings;
    @Mock OrderRepository orders;
    @Mock CatalogClient catalog;
    AdminAnalyticsService service;
    final LocalDate day = LocalDate.of(2026, 1, 15);

    @BeforeEach void setup() { service = new AdminAnalyticsServiceImpl(bookings, orders, catalog); }
    BigDecimal money(String value) { return new BigDecimal(value); }
    void assertMoney(String expected, BigDecimal actual) { assertEquals(0, money(expected).compareTo(actual)); }
    Booking booking(long showId, BookingStatus status, String amount) {
        Booking b = Booking.builder().id(showId).cinemaId(7L).cinemaNameSnapshot("Rạp A")
                .movieId(5L).movieTitleSnapshot("Phim A").showtimeId(showId)
                .showtimeStartSnapshot(day.atTime(10, 0)).showtimeEndSnapshot(day.atTime(12, 0))
                .paidAt(day.atTime(8, 0)).totalAmount(money(amount)).status(status).build();
        ReflectionTestUtils.setField(b, "createdAt", day.atTime(7, 0));
        b.getSeats().add(BookingSeat.builder().unitPrice(money("100")).build());
        return b;
    }
    Order order(Long bookingId, String status, String amount) {
        Order o = Order.builder().cinemaId(7L).cinemaName("Rạp A").bookingId(bookingId)
                .paidAt(day.atTime(9, 0)).status(status).totalAmount(money(amount)).build();
        ReflectionTestUtils.setField(o, "createdAt", day.atTime(9, 0));
        o.getItems().add(OrderItem.builder().productNameSnapshot("Nước").quantity(2).lineTotal(money("60")).build());
        return o;
    }
    void seed(List<Booking> list, List<Order> food) {
        when(bookings.findForAnalytics(day.atStartOfDay(), day.plusDays(1).atStartOfDay(), 7L)).thenReturn(list);
        when(orders.findForAnalytics(day.atStartOfDay(), day.plusDays(1).atStartOfDay(), 7L)).thenReturn(food);
    }
    void shows(List<ReportingShowtimeDto> shows) { when(catalog.getReportingShowtimes(day, day, 7L)).thenReturn(shows); }
    ReportingShowtimeDto show(long id, long capacity) {
        return new ReportingShowtimeDto(id, 5L, "Phim A", 7L, "Rạp A", 3L, "Phòng 3", day.atTime(10, 0), day.atTime(12, 0), capacity);
    }

    @Test void reconcilesDiscountedTicketFoodStandaloneAndAddonWithoutDoubleCounting() {
        Booking b = booking(1L, BookingStatus.PAID, "120");
        b.getFoodItems().add(BookingProductItem.builder().productNameSnapshot("Bắp").quantity(1).lineTotal(money("50")).build());
        seed(List.of(b), List.of(order(null, "PAID", "54"), order(1L, "PICKED_UP", "30"), order(null, "CANCELLED", "99")));
        shows(List.of(show(1L, 80), show(2L, 20)));
        var data = service.getAnalytics(day, day, 7L);
        assertMoney("204", data.summary().totalRevenue());
        assertMoney("80", data.summary().ticketRevenue());
        assertMoney("124", data.summary().foodRevenue());
        assertEquals(3, data.summary().totalTransactions());
        assertEquals(1, data.summary().ticketsSold());
        assertEquals(100, data.summary().totalCapacity());
        assertEquals(1.0, data.summary().occupancyRate());
        assertEquals(2, data.showtimes().size()); // Includes a show without any booking.
        assertEquals(20, data.showtimes().get(1).remaining());
        assertEquals(3, data.concessions().totalOrders());
        assertEquals(5, data.concessions().totalItemsSold());
        assertMoney("124", data.concessions().lines().stream().map(l -> l.revenue()).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertMoney("124", data.concessions().sources().stream().map(l -> l.revenue()).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertMoney("204", data.daily().getFirst().totalRevenue());
        assertMoney("204", data.cinemas().getFirst().totalRevenue());
        assertMoney("80", data.topMovies().getFirst().getRevenue());
        assertNull(data.concessions().grossProfit());
        assertNull(data.concessions().profitMargin());
        assertNull(data.concessions().lowStockCount());
        assertNull(data.concessions().outOfStockCount());
        verify(bookings).findForAnalytics(day.atStartOfDay(), day.plusDays(1).atStartOfDay(), 7L);
    }

    @Test void countsNoShowsOnlyForFinishedShowsAndUsesPaymentDateForRevenue() {
        Booking past = booking(1L, BookingStatus.PAID, "100");
        ReflectionTestUtils.setField(past, "createdAt", day.minusDays(5).atStartOfDay());
        Booking future = booking(2L, BookingStatus.PAID, "100");
        future.setShowtimeEndSnapshot(LocalDateTime.now().plusDays(5));
        Booking used = booking(3L, BookingStatus.USED, "100");
        used.setPaidAt(day.minusDays(1).atTime(23, 59));
        Booking refunded = booking(4L, BookingStatus.REFUNDED, "80");
        refunded.setRefundedAt(day.atTime(10, 0));
        seed(List.of(past, future, used, refunded), List.of()); shows(List.of(show(1L, 100)));
        var data = service.getAnalytics(day, day, 7L);
        assertMoney("200", data.summary().totalRevenue());
        assertEquals(2, data.noShows().finishedPaidBookings());
        assertEquals(1, data.noShows().noShowBookings());
        assertEquals(50, data.noShows().noShowRatePercent());
        assertMoney("80", data.summary().refundAmount());
        assertEquals(1, data.summary().refundedBookings());
    }

    @Test void neverUsesMidnightOfFollowingDayOrPendingOrders() {
        Booking boundary = booking(1L, BookingStatus.PAID, "100"); boundary.setPaidAt(day.plusDays(1).atStartOfDay());
        boundary.getFoodItems().add(BookingProductItem.builder().productNameSnapshot("Bắp").quantity(1).lineTotal(money("50")).build());
        seed(List.of(boundary, booking(2L, BookingStatus.EXPIRED, "100")), List.of(order(null, "PENDING_PAYMENT", "50")));
        var data = service.getConcessions(day, day, 7L);
        assertMoney("0", data.totalRevenue()); assertEquals(0, data.totalOrders()); assertEquals(1, data.daily().size());
        verifyNoInteractions(catalog); // F&B remains available without the Catalog service.
    }

    @Test void rejectsInvalidAndOversizedRangesBeforeQuerying() {
        assertThrows(BadRequestException.class, () -> service.getAnalytics(day, day.minusDays(1), null));
        assertThrows(BadRequestException.class, () -> service.getConcessions(day, day.plusDays(366), null));
        verifyNoInteractions(bookings, orders, catalog);
    }

    @Test void surfacesCatalogFailureInsteadOfReturningFabricatedCapacity() {
        seed(List.of(), List.of());
        when(catalog.getReportingShowtimes(day, day, 7L)).thenThrow(new IllegalStateException("offline"));
        assertThrows(IllegalStateException.class, () -> service.getAnalytics(day, day, 7L));
    }
}
