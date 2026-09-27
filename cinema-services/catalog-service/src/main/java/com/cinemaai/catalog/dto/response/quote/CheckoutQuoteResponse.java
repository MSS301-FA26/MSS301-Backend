package com.cinemaai.catalog.dto.response.quote;

import com.cinemaai.catalog.enums.SeatType;
import com.cinemaai.catalog.enums.TicketType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record CheckoutQuoteResponse(
        String quoteId,
        Instant validUntil,
        ShowtimeSnapshot showtime,
        List<SeatSnapshot> seats,
        List<TicketSnapshot> tickets,
        List<FoodSnapshot> foods,
        List<FoodSnapshot> foodItems,
        BigDecimal ticketSubtotal,
        BigDecimal foodSubtotal,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal cinePointsDiscount,
        BigDecimal fees,
        BigDecimal tax,
        BigDecimal total,
        String voucherMessage,
        MovieSummary movie,
        CinemaSummary cinema
) {
    public CheckoutQuoteResponse(
            String quoteId,
            Instant validUntil,
            ShowtimeSnapshot showtime,
            List<SeatSnapshot> seats,
            List<TicketSnapshot> tickets,
            List<FoodSnapshot> foods,
            BigDecimal ticketSubtotal,
            BigDecimal foodSubtotal,
            BigDecimal subtotal
    ) {
        this(
                quoteId,
                validUntil,
                showtime,
                seats,
                tickets,
                foods,
                foods,
                ticketSubtotal,
                foodSubtotal,
                subtotal,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                subtotal,
                null,
                showtime != null ? new MovieSummary(showtime.movieId(), showtime.movieTitle(), showtime.posterUrl(), null, null) : null,
                showtime != null ? new CinemaSummary(null, showtime.cinemaName(), null, showtime.roomName()) : null
        );
    }

    public record MovieSummary(Long id, String title, String posterUrl, String ageRating, Integer durationMinutes) {}
    public record CinemaSummary(Long id, String name, String address, String roomName) {}
    public record ShowtimeSnapshot(Long showtimeId, Long movieId, String movieTitle, String posterUrl,
            String cinemaName, String roomName, LocalDateTime startTime) {}
    public record SeatSnapshot(Long seatId, String seatLabel, SeatType seatType, BigDecimal unitPrice) {}
    public record TicketSnapshot(Long seatId, TicketType ticketType, int quantity,
            BigDecimal unitPrice, BigDecimal lineTotal) {}
    public record FoodSnapshot(Long productId, boolean isCombo, String productName, BigDecimal unitPrice,
            int quantity, BigDecimal lineTotal) {}
}
