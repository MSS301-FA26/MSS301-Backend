package com.sba301.cinemaai.dto.response.report;

public record PeakHourResponse(
        int hour,
        long ticketsSold
) {
}
