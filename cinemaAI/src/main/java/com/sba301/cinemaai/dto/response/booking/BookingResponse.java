package com.sba301.cinemaai.dto.response.booking;

import com.sba301.cinemaai.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingCode,
        Long userId,
        Long showtimeId,
        Long movieId,
        String movieTitle,
        String posterUrl,
        String roomName,
        String cinemaName,
        LocalDateTime showtimeStart,
        BookingStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        int loyaltyPointsRedeemed,
        BigDecimal totalAmount,
        LocalDateTime holdExpiresAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt,
        LocalDateTime checkedInAt,
        LocalDateTime refundRequestedAt,
        LocalDateTime refundedAt,
        String refundReason,
        String refundMethod,
        boolean bulkRefund,
        int refundRetryAttempts,
        LocalDateTime lastRefundAttemptAt,
        String qrCode,
        String paymentAccount,
        String customerName,
        String customerPhone,
        String userEmail,
        LocalDateTime showtimeEnd,
        LocalDateTime createdAt,
        List<BookingSeatResponse> seats,
        List<BookingTicketResponse> tickets,
        List<BookingFoodResponse> foods
) {
}
