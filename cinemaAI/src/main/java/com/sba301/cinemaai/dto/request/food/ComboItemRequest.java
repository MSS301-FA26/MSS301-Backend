package com.sba301.cinemaai.dto.request.food;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ComboItemRequest(
        @NotNull(message = "Food item ID is required")
        Long foodItemId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {
}
