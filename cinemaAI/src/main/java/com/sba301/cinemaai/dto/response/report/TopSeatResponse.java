package com.sba301.cinemaai.dto.response.report;

public record TopSeatResponse(
        String rowLabel,
        int seatNumber,
        String seatType,
        long timesPurchased
) {
}
