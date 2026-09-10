package com.sba301.cinemaai.dto.request.booking;

import jakarta.validation.constraints.Min;

public record BookingFoodRequest(
        Long foodItemId,
        Long foodComboId,

        @Min(value = 1, message = "Quantity must be positive")
        int quantity
) {
}
