package com.cinemaai.booking.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public class CatalogQuoteDto {

    public record Request(
            Long showtimeId,
            List<Long> seatIds,
            List<Ticket> tickets,
            List<Food> foods
    ) {
        public record Ticket(Long seatId, String ticketType, Integer viewerAge, int quantity) {}
        public record Food(Long productId, Boolean isCombo, int quantity) {}
    }

    public record Response(
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
        public record ShowtimeSnapshot(
                Long showtimeId,
                Long movieId,
                String movieTitle,
                String posterUrl,
                String cinemaName,
                String roomName,
                LocalDateTime startTime
        ) {}

        public record SeatSnapshot(
                Long seatId,
                String seatLabel,
                String seatType,
                BigDecimal unitPrice
        ) {}

        public record TicketSnapshot(
                Long seatId,
                String ticketType,
                int quantity,
                BigDecimal unitPrice,
                BigDecimal lineTotal
        ) {}

        public record FoodSnapshot(
                Long productId,
                boolean isCombo,
                String productName,
                BigDecimal unitPrice,
                int quantity,
                BigDecimal lineTotal
        ) {}
    }
}
