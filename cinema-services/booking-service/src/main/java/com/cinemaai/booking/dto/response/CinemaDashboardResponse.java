package com.cinemaai.booking.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CinemaDashboardResponse(
        Long cinemaId,
        BigDecimal totalRevenue,
        long totalPaidTickets,
        long totalCancelledTickets,
        long totalRefundedTickets,
        BigDecimal totalRefundedAmount,
        double occupancyRate,
        List<CinemaMetricDto> cinemaMetrics,
        List<TicketAuditLogResponse> recentAuditLogs,
        List<BookingResponse> pendingRefundTickets
) {
    public record CinemaMetricDto(
            Long cinemaId,
            long ticketsSold,
            BigDecimal revenue
    ) {}
}
