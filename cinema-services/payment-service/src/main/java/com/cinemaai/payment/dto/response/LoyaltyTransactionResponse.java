package com.cinemaai.payment.dto.response;

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
        String type,
        int pointsDelta,
        int balanceAfter,
        LocalDateTime occurredAt,
        String note
) {}
