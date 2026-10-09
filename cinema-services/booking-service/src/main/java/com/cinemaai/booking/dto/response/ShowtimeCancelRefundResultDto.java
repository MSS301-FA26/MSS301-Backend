package com.cinemaai.booking.dto.response;

import java.math.BigDecimal;

public record ShowtimeCancelRefundResultDto(
        Long showtimeId,
        int totalBookingsProcessed,
        int refundedTicketsCount,
        int cancelledTicketsCount,
        BigDecimal totalRefundedAmount,
        String message
) {}
