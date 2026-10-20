package com.sba301.cinemaai.dto.response.food;

public record FoodCategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        String imageUrl,
        int sortOrder,
        String status,
        long productCount
) {
}
