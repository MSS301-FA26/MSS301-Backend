package com.cinemaai.catalog.dto.response.product;

import com.cinemaai.catalog.enums.ProductStatus;
import java.math.BigDecimal;

public record ProductBundleResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        ProductStatus status
) {
}
