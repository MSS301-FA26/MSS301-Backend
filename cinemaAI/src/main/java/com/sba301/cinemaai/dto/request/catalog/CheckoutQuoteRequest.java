package com.sba301.cinemaai.dto.request.catalog;

import com.sba301.cinemaai.dto.request.booking.BookingFoodRequest;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CheckoutQuoteRequest(
        Long bookingSessionId,
        @NotNull(message = "showtimeId is required")
        Long showtimeId,
        List<Long> seatIds,
        List<BookingFoodRequest> foods,
        String voucherCode,
        Integer cinePointsToUse
) {
}
