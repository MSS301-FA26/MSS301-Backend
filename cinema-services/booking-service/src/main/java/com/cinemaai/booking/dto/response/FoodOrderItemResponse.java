package com.cinemaai.booking.dto.response;

import java.math.BigDecimal;

public record FoodOrderItemResponse(
        Long id,
        Long productId,
        Long foodItemId,
        Long foodComboId,
        boolean isCombo,
        String name,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}