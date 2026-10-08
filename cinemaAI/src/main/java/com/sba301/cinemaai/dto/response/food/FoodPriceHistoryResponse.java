package com.sba301.cinemaai.dto.response.food;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FoodPriceHistoryResponse(
        Long id,
        Long foodItemId,
        Long foodComboId,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        BigDecimal difference,
        String changedBy,
        LocalDateTime changedAt
) {
}
