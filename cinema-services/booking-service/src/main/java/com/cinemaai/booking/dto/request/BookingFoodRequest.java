package com.cinemaai.booking.dto.request;

import jakarta.validation.constraints.Min;

public record BookingFoodRequest(
        Long foodItemId,
        Long foodComboId,
        Long productId,
        Boolean isCombo,
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        int quantity
) {
    public BookingFoodRequest(Long foodItemId, Long foodComboId, int quantity) {
        this(foodItemId, foodComboId, null, null, quantity);
    }
}