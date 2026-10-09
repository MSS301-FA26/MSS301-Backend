package com.cinemaai.booking.client.dto;

import java.math.BigDecimal;
import java.util.List;

public class CatalogFoodQuoteDto {

    public record Request(
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

    public record Response(
            List<ItemSnapshot> items,
            BigDecimal totalAmount
    ) {
        public record ItemSnapshot(
                Long productId,
                boolean isCombo,
                String productName,
                BigDecimal unitPrice,
                int quantity,
                BigDecimal lineTotal
        ) {}
    }
}