package com.cinemaai.booking.dto.response.report;

public record TopSeatResponse(
        String rowLabel,
        int seatNumber,
        String seatType,
        long timesPurchased
) {
}

