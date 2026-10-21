package com.cinemaai.catalog.dto.response.product;

public record ProductCategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        Integer sortOrder,
        String status
) {
}
