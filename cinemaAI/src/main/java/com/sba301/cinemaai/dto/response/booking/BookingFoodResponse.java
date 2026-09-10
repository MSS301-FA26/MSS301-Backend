package com.sba301.cinemaai.dto.response.booking;

import java.math.BigDecimal;

public record BookingFoodResponse(
        Long foodItemId,
        Long foodComboId,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
