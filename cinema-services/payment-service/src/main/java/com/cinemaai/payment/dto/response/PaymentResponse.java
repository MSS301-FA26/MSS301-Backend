package com.cinemaai.payment.dto.response;

import com.cinemaai.payment.enums.PaymentProvider;
import com.cinemaai.payment.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long bookingId,
        Long foodOrderId,
        Long userId,
        PaymentProvider provider,
        String transactionId,
        BigDecimal amount,
        PaymentStatus status,
        String paymentUrl,
        String paymentAccountLabel,
        LocalDateTime paidAt,
        BigDecimal refundAmount,
        LocalDateTime refundedAt,
        LocalDateTime createdAt
) {}
