package com.cinemaai.catalog.dto.response.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductQuoteResponse(
        List<ProductSnapshot> items,
        BigDecimal totalAmount
) {
    public record ProductSnapshot(
            Long productId,
            boolean isCombo,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ) {}
}
