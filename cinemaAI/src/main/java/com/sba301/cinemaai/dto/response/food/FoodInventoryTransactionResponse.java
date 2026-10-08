package com.sba301.cinemaai.dto.response.food;

import com.sba301.cinemaai.enums.FoodInventoryTxType;
import java.time.LocalDateTime;

public record FoodInventoryTransactionResponse(
        Long id,
        Long cinemaId,
        String cinemaName,
        Long foodItemId,
        String foodItemName,
        String foodItemSku,
        FoodInventoryTxType type,
        int quantity,
        int beforeQuantity,
        int afterQuantity,
        String reason,
        String referenceId,
        String createdBy,
        LocalDateTime createdAt
) {
}
