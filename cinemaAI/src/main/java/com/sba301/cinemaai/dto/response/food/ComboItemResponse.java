package com.sba301.cinemaai.dto.response.food;

import java.math.BigDecimal;

public record ComboItemResponse(
        Long id,
        Long foodItemId,
        String foodItemName,
        String foodItemSku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}
