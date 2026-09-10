package com.sba301.cinemaai.dto.request.booking;

import jakarta.validation.Valid;
import java.util.List;

public record UpdateHoldingBookingRequest(
        @Valid
        List<BookingFoodRequest> foods,
        Integer loyaltyPointsToRedeem
) {
}
