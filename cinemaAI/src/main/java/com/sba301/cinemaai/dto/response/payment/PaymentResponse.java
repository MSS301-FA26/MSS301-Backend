package com.sba301.cinemaai.dto.response.payment;

import com.sba301.cinemaai.enums.PaymentProvider;
import com.sba301.cinemaai.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long bookingId,
        PaymentProvider provider,
        String transactionId,
        BigDecimal amount,
        PaymentStatus status,
        String paymentUrl,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}
