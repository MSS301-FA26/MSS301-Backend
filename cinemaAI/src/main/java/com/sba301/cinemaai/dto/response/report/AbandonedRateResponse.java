package com.sba301.cinemaai.dto.response.report;

import java.time.LocalDate;

public record AbandonedRateResponse(
        LocalDate from,
        LocalDate to,
        long expiredBookings,
        long cancelledBookings,
        long paidBookings,
        double abandonedRatePercent
) {
}
