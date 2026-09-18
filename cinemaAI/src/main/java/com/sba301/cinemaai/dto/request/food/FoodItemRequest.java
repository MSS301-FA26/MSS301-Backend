package com.sba301.cinemaai.dto.request.food;

import com.sba301.cinemaai.enums.FoodItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FoodItemRequest(
        String sku,

        @NotBlank(message = "Food item name is required")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
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

        Boolean stockTracking,

        Integer lowStockThreshold,

        Integer initialStock,

        Long cinemaId
) {
    // Backward-compatible constructor for tests and older callers
    public FoodItemRequest(String name, String description, BigDecimal price, String imageUrl, FoodItemStatus status) {
        this(null, name, description, null, BigDecimal.ZERO, price, imageUrl, status, true, 10, null, null);
    }
}
