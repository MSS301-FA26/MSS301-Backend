package com.cinemaai.catalog.dto.response.food;

import com.cinemaai.catalog.enums.FoodItemStatus;
import java.math.BigDecimal;

public record FoodItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        FoodItemStatus status,
        Long categoryId,
        String categoryName
) {
    public FoodItemResponse(Long id, String name, String description, BigDecimal price, String imageUrl, FoodItemStatus status) {
        this(id, name, description, price, imageUrl, status, null, null);
    }
}
