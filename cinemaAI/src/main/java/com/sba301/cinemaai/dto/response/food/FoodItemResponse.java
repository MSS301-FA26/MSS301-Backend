package com.sba301.cinemaai.dto.response.food;

import com.sba301.cinemaai.enums.FoodItemStatus;
import com.sba301.cinemaai.enums.FoodStockStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FoodItemResponse(
        Long id,
        String sku,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        BigDecimal costPrice,
        BigDecimal price,
        String imageUrl,
        FoodItemStatus status,
        boolean stockTracking,
        int lowStockThreshold,
        int totalStock,
        FoodStockStatus stockStatus,
        LocalDateTime deletedAt,
        BigDecimal grossProfit,
        Double marginPercentage
) {
    // Backward-compatible constructor
    public FoodItemResponse(
            Long id,
            String name,
            String description,
            BigDecimal price,
            String imageUrl,
            FoodItemStatus status
    ) {
        this(id, null, name, description, null, null, BigDecimal.ZERO, price, imageUrl, status, true, 10, 0, FoodStockStatus.IN_STOCK, null, BigDecimal.ZERO, 0.0);
    }
}
