package com.sba301.cinemaai.dto.response.food;

import com.sba301.cinemaai.enums.FoodItemStatus;
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
