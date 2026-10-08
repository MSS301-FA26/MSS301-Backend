package com.sba301.cinemaai.dto.response.food;

import com.sba301.cinemaai.enums.FoodItemStatus;
import com.sba301.cinemaai.enums.FoodStockStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FoodComboResponse(
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
        int maxAvailableCombos,
        FoodStockStatus stockStatus,
        LocalDateTime deletedAt,
        BigDecimal regularPriceSum,
        BigDecimal savingsAmount,
        BigDecimal grossProfit,
        Double marginPercentage,
        List<ComboItemResponse> items
) {
    // Backward-compatible constructor
    public FoodComboResponse(
            Long id,
            String name,
            String description,
            BigDecimal price,
            String imageUrl,
            FoodItemStatus status
    ) {
        this(id, null, name, description, null, null, BigDecimal.ZERO, price, imageUrl, status, 0, FoodStockStatus.IN_STOCK, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, List.of());
    }
}
