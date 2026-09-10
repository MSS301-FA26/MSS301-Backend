package com.sba301.cinemaai.dto.request.food;

import com.sba301.cinemaai.enums.FoodItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FoodComboRequest(
        @NotBlank(message = "Food combo name is required")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
        BigDecimal price,

        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String imageUrl,

        FoodItemStatus status
) {
}
