package com.sba301.cinemaai.dto.response.report;

import java.math.BigDecimal;
import java.util.List;

public record ShowtimeIncidentReportResponse(
        long totalCancelledShowtimes,
        long totalRefundedBookings,
        BigDecimal totalRefundedAmount,
        long totalRefundedUsers,
        List<ShowtimeIncidentItem> incidents
) {}
