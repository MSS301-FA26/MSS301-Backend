package com.cinemaai.booking.dto.response.report;

import java.time.LocalDate;

public record NoShowReportResponse(
        LocalDate from,
        LocalDate to,
        long noShowBookings,
        long finishedPaidBookings,
        double noShowRatePercent
) {
}

