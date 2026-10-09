package com.cinemaai.booking.dto.response;

import java.math.BigDecimal;

public record ShowtimeBookingSummaryDto(
        Long showtimeId,
        int soldTicketsCount,
        int paidBookingsCount,
        int holdingTicketsCount,
        BigDecimal totalRefundAmount
) {
}
