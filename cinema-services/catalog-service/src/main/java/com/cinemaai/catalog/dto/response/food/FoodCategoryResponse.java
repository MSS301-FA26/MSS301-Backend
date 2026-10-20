package com.cinemaai.catalog.dto.response.food;

public record FoodCategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        Integer sortOrder,
        String status
) {
}
