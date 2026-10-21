package com.cinemaai.catalog.dto.response.product;

import com.cinemaai.catalog.enums.ProductStatus;
import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        ProductStatus status,
        Long categoryId,
        String categoryName
) {
    public ProductResponse(Long id, String name, String description, BigDecimal price, String imageUrl, ProductStatus status) {
        this(id, name, description, price, imageUrl, status, null, null);
    }
}
