package com.cinemaai.booking.dto.response.report;

public record PeakHourResponse(
        int hour,
        long ticketsSold
) {
}

