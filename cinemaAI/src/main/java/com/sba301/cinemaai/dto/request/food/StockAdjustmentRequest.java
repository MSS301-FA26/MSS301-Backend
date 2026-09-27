package com.sba301.cinemaai.dto.request.food;

import com.sba301.cinemaai.enums.FoodInventoryTxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(
        @NotNull(message = "Cinema ID is required")
        Long cinemaId,

        @NotNull(message = "Food item ID is required")
        Long foodItemId,

        @NotNull(message = "Adjustment type is required")
        FoodInventoryTxType type,

        Integer quantityDelta,

        Integer newQuantity,

        @NotBlank(message = "Adjustment reason is mandatory")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
