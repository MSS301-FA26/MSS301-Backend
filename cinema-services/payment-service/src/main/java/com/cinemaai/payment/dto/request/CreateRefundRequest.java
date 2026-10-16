package com.cinemaai.payment.dto.request;

import java.math.BigDecimal;

public record CreateRefundRequest(
        Long paymentId,
        Long bookingId,
        Long foodOrderId,
        BigDecimal amount,
        String reason
) {}
