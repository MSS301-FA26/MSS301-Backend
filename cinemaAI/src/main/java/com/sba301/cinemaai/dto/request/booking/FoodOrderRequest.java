package com.sba301.cinemaai.dto.request.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record FoodOrderRequest(
        @NotEmpty(message = "At least one food item is required")
        @Valid
        List<BookingFoodRequest> foods
) {
}
