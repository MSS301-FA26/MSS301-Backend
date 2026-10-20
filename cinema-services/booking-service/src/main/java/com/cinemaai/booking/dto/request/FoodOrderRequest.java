package com.cinemaai.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record FoodOrderRequest(
        @NotEmpty(message = "Danh sách món không được để trống")
        @Valid
        List<BookingFoodRequest> foods,
        String promotionCode,
        Long cinemaId,
        String cinemaName,
        String cinemaAddress
) {
    public FoodOrderRequest(List<BookingFoodRequest> foods) {
        this(foods, null, null, null, null);
    }

    public FoodOrderRequest(List<BookingFoodRequest> foods, String promotionCode) {
        this(foods, promotionCode, null, null, null);
    }
}
