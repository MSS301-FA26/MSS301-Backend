package com.sba301.cinemaai.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ShowtimeIncidentItem(
        Long showtimeId,
        String movieTitle,
        String roomName,
        String cinemaName,
        LocalDateTime showtimeStart,
        String cancellationReason,
        LocalDateTime cancelledAt,
        long refundedBookingsCount,
        BigDecimal totalRefundedAmount,
        List<IncidentRefundedUser> refundedUsers
) {}
