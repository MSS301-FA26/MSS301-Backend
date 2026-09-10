package com.sba301.cinemaai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sba301.cinemaai.repository.BookingFoodItemRepository;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.BookingSeatRepository;
import com.sba301.cinemaai.repository.PaymentRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportServiceImplTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final BookingFoodItemRepository foodItemRepository = mock(BookingFoodItemRepository.class);
    private final ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
    private final BookingSeatRepository bookingSeatRepository = mock(BookingSeatRepository.class);
    private final ReportServiceImpl service = new ReportServiceImpl(
            paymentRepository,
            bookingRepository,
            foodItemRepository,
            showtimeRepository,
            bookingSeatRepository
    );

    @Test
    void concessionSalesAggregatesOrdersByDayAndSource() {
        LocalDate from = LocalDate.of(2026, 7, 22);
        LocalDate to = LocalDate.of(2026, 7, 23);
        LocalDateTime rangeFrom = from.atStartOfDay();
        LocalDateTime rangeTo = to.plusDays(1).atStartOfDay();

        when(foodItemRepository.concessionSales(rangeFrom, rangeTo)).thenReturn(List.of(
                new Object[] {"Combo Couple", 2L, new BigDecimal("200000")},
                new Object[] {"Nuoc suoi", 1L, new BigDecimal("100000")}
        ));
        when(foodItemRepository.concessionTransactions(rangeFrom, rangeTo)).thenReturn(List.of(
                new Object[] {
                        LocalDateTime.of(2026, 7, 22, 10, 0),
                        "WITH_TICKET",
                        11L,
                        2L,
                        new BigDecimal("200000")
                },
                new Object[] {
                        LocalDateTime.of(2026, 7, 22, 18, 0),
                        "STANDALONE",
                        12L,
                        1L,
                        new BigDecimal("100000")
                }
        ));

        var result = service.getConcessionSales(from, to);

        assertThat(result.totalOrders()).isEqualTo(2);
        assertThat(result.totalItemsSold()).isEqualTo(3);
        assertThat(result.totalRevenue()).isEqualByComparingTo("300000");
        assertThat(result.averageOrderValue()).isEqualByComparingTo("150000.00");
        assertThat(result.daily()).hasSize(2);
        assertThat(result.daily().get(0).orderCount()).isEqualTo(2);
        assertThat(result.daily().get(0).quantity()).isEqualTo(3);
        assertThat(result.daily().get(1).orderCount()).isZero();
        assertThat(result.sources())
                .extracting(source -> source.source(), source -> source.orderCount())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("WITH_TICKET", 1L),
                        org.assertj.core.groups.Tuple.tuple("ADD_ON", 0L),
                        org.assertj.core.groups.Tuple.tuple("STANDALONE", 1L)
                );
    }
}
