package com.cinemaai.catalog.dto.response.quote;

import com.cinemaai.catalog.enums.SeatType;
import com.cinemaai.catalog.enums.TicketType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record CheckoutQuoteResponse(String quoteId, Instant validUntil, ShowtimeSnapshot showtime,
        List<SeatSnapshot> seats, List<TicketSnapshot> tickets, List<FoodSnapshot> foods,
        BigDecimal ticketSubtotal, BigDecimal foodSubtotal, BigDecimal subtotal) {
    public record ShowtimeSnapshot(Long showtimeId, Long movieId, String movieTitle, String posterUrl,
            String cinemaName, String roomName, LocalDateTime startTime) {}
    public record SeatSnapshot(Long seatId, String seatLabel, SeatType seatType, BigDecimal unitPrice) {}
    public record TicketSnapshot(Long seatId, TicketType ticketType, int quantity,
            BigDecimal unitPrice, BigDecimal lineTotal) {}
    public record FoodSnapshot(Long productId, boolean isCombo, String productName, BigDecimal unitPrice,
            int quantity, BigDecimal lineTotal) {}
}
