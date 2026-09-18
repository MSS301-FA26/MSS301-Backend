package com.sba301.cinemaai.dto.request.food;

import com.sba301.cinemaai.enums.FoodItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record FoodComboRequest(
        String sku,

        @NotBlank(message = "Food combo name is required")
        String name,

        String description,

        Long categoryId,

        @DecimalMin(value = "0.0", message = "Cost price must be non-negative")
        BigDecimal costPrice,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
        BigDecimal price,

        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String imageUrl,

        FoodItemStatus status,

        List<ComboItemRequest> items
) {
    // Backward-compatible constructor
    public FoodComboRequest(String name, String description, BigDecimal price, String imageUrl, FoodItemStatus status) {
        this(null, name, description, null, BigDecimal.ZERO, price, imageUrl, status, List.of());
    }
}
