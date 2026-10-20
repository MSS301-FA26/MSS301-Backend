package com.cinemaai.booking.dto.response.report;

import java.math.BigDecimal;

public record CinemaOverviewReportResponse(
        Long cinemaId,
        String cinemaName,
        BigDecimal todayRevenue,
        BigDecimal totalRevenue,
        long todayTicketsSold,
        long totalTicketsSold,
        long todayBookings,
        long totalBookings,
        double occupancyRate,
        int activeRoomsCount,
        int lowStockCount,
        int outOfStockCount,
        int todayShowtimesCount
) {
}

