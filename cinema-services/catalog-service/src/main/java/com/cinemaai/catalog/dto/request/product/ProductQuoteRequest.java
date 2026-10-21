package com.cinemaai.catalog.dto.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record ProductQuoteRequest(
        @NotEmpty List<@Valid Item> foods
) {
    public record Item(
            @Positive Long foodItemId,
            @Positive Long foodComboId,
            @Positive Long productId,
            Boolean isCombo,
            @Positive Integer quantity
    ) {}
}
