package com.cinemaai.payment.dto.request;

import java.math.BigDecimal;

public record RefundBookingPointsRequest(
        Long userId,
        Long bookingId,
        String bookingCode,
        BigDecimal amount,
        Integer redeemedPoints,
        String reason
) {}
