package com.sba301.cinemaai.dto.response.food;

import com.sba301.cinemaai.enums.FoodStockStatus;
import java.time.LocalDateTime;

public record FoodInventoryResponse(
        Long id,
        Long cinemaId,
        String cinemaName,
        Long foodItemId,
        String foodItemName,
        String foodItemSku,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        int lowStockThreshold,
        FoodStockStatus stockStatus,
        LocalDateTime updatedAt
) {
}
