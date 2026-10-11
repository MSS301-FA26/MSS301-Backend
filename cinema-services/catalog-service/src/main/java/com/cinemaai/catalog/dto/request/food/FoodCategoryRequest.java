package com.cinemaai.catalog.dto.request.food;

import jakarta.validation.constraints.NotBlank;

public record FoodCategoryRequest(
        @NotBlank(message = "Category code cannot be blank")
        String code,
        @NotBlank(message = "Category name cannot be blank")
        String name,
        String description,
        Integer sortOrder,
        String status
) {
}
