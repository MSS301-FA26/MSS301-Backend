package com.sba301.cinemaai.dto.response.loyalty;

import com.sba301.cinemaai.entity.LoyaltyPointTransaction;
import com.sba301.cinemaai.enums.LoyaltyPointType;
import java.time.LocalDateTime;

public record LoyaltyTransactionResponse(
        Long pointTransactionId,
        Long userId,
        String customerName,
        String customerPhone,
        String customerEmail,
        Long bookingId,
        String bookingCode,
        String movieTitle,
        LocalDateTime showtimeStart,
        String roomName,
        LoyaltyPointType type,
        int pointsDelta,
        int balanceAfter,
        LocalDateTime occurredAt,
        String note
) {
    public static LoyaltyTransactionResponse from(LoyaltyPointTransaction tx) {
        var booking = tx.getBooking();
        return new LoyaltyTransactionResponse(
                tx.getId(),
                tx.getUser().getId(),
                tx.getUser().getFullName(),
                tx.getUser().getPhone(),
                tx.getUser().getEmail(),
                booking == null ? null : booking.getId(),
                booking == null ? null : booking.getBookingCode(),
                booking == null ? null : booking.getShowtime().getMovie().getTitle(),
                booking == null ? null : booking.getShowtime().getStartTime(),
                booking == null ? null : booking.getShowtime().getRoom().getName(),
                tx.getType(),
                tx.getPointsDelta(),
                tx.getBalanceAfter(),
                tx.getOccurredAt(),
                tx.getNote()
        );
    }
}
