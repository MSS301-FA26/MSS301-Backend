package com.cinemaai.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentSucceededEvent(
        String eventId,
        Long paymentId,
        Long bookingId,
        Long foodOrderId,
        Long userId,
        BigDecimal amount,
        String provider,
        LocalDateTime paidAt
) {}
