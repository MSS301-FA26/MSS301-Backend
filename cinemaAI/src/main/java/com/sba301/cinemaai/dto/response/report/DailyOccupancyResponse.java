package com.sba301.cinemaai.dto.response.report;

import java.time.LocalDate;

public record DailyOccupancyResponse(
        LocalDate date,
        long ticketsSold,
        long totalCapacity,
        long totalShowtimes,
        double occupancyRate
) {
}
