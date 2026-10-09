package com.cinemaai.catalog.dto.request.food;

import java.util.List;

public record FoodQuoteRequest(
        List<Item> foods
) {
    public record Item(
            Long foodItemId,
            Long foodComboId,
            Long productId,
            Boolean isCombo,
            Integer quantity
    ) {}
}
