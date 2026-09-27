package com.sba301.cinemaai.dto.response.catalog;

import com.sba301.cinemaai.enums.SeatType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CheckoutQuoteResponse(
        MovieSummary movie,
        CinemaSummary cinema,
        ShowtimeSummary showtime,
        List<SeatQuoteItem> seats,
        List<FoodQuoteItem> foodItems,
        BigDecimal ticketSubtotal,
        BigDecimal foodSubtotal,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal cinePointsDiscount,
        BigDecimal fees,
        BigDecimal tax,
        BigDecimal total,
        String voucherMessage
) {
    public record MovieSummary(Long id, String title, String posterUrl, String ageRating, Integer durationMinutes) {}
    public record CinemaSummary(Long id, String name, String address, String roomName) {}
    public record ShowtimeSummary(Long id, LocalDateTime startTime, LocalDateTime endTime) {}
    public record SeatQuoteItem(Long seatId, String seatCode, String rowLabel, int seatNumber, SeatType seatType, BigDecimal unitPrice) {}
    public record FoodQuoteItem(Long id, String name, int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {}
}
