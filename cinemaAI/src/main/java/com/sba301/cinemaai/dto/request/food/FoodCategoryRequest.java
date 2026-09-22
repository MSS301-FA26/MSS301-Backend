package com.sba301.cinemaai.dto.request.food;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FoodCategoryRequest(
        @NotBlank(message = "Category code is required")
        @Size(max = 50, message = "Category code must be at most 50 characters")
        String code,

        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must be at most 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String imageUrl,

        Integer sortOrder,

        String status
) {
}
