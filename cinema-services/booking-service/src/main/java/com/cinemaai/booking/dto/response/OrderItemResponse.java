package com.cinemaai.booking.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        Long foodItemId,
        Long foodComboId,
        boolean isCombo,
        String name,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        boolean pickedUp
) {
    public OrderItemResponse(
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
    ) {
        this(id, productId, foodItemId, foodComboId, isCombo, name, productName, quantity, unitPrice, lineTotal, false);
    }
}
