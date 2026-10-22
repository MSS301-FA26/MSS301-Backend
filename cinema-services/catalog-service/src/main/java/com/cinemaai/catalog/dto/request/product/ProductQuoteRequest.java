package com.cinemaai.catalog.dto.request.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record ProductQuoteRequest(
        @NotEmpty List<@Valid ProductSelection> foods
) {
    @JsonCreator
    public ProductQuoteRequest(
            @JsonProperty("foods") List<ProductSelection> foods,
            @JsonProperty("products") List<ProductSelection> products
    ) {
        this(normalize(foods, products));
    }

    private static List<ProductSelection> normalize(
            List<ProductSelection> foods,
            List<ProductSelection> products
    ) {
        if (foods != null && products != null) {
            throw new IllegalArgumentException("Specify either foods or products, not both");
        }
        return foods != null ? foods : products;
    }

    public record ProductSelection(
            @Positive Long foodItemId,
            @Positive Long foodComboId,
            @Positive Long productId,
            Boolean isCombo,
            @Positive Integer quantity
    ) {}
}
