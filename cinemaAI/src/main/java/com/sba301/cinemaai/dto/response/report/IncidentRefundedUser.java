package com.sba301.cinemaai.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IncidentRefundedUser(
        Long bookingId,
        String bookingCode,
        Long userId,
        String userName,
        String userEmail,
        String userPhone,
        BigDecimal amount,
        String refundMethod,
        LocalDateTime refundedAt,
        String seatLabels
) {}
