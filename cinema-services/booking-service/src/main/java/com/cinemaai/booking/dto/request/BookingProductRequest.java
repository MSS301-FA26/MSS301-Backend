package com.cinemaai.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record BookingProductRequest(
        @Positive Long foodItemId,
        @Positive Long foodComboId,
        @Positive Long productId,
        Boolean isCombo,
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        int quantity
) {
    public BookingProductRequest(Long foodItemId, Long foodComboId, int quantity) {
        this(foodItemId, foodComboId, null, null, quantity);
    }
}
