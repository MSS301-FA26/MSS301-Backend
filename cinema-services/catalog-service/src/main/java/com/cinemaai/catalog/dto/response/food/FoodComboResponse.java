package com.cinemaai.catalog.dto.response.food;

import com.cinemaai.catalog.enums.FoodItemStatus;
import java.math.BigDecimal;

public record FoodComboResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        FoodItemStatus status
) {
}
