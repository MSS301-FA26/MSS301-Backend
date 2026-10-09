package com.cinemaai.payment.dto.request;

import java.math.BigDecimal;

public record AwardBookingPointsRequest(
        Long userId,
        Long bookingId,
        String bookingCode,
        BigDecimal amount,
        Integer redeemedPoints
) {}
