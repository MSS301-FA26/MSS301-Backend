package com.sba301.cinemaai.dto.response.promotion;

import java.math.BigDecimal;

public record PromotionStatsResponse(
        long totalPromotions,
        long activePromotions,
        long totalUsages,
        BigDecimal totalDiscountGiven
) {
}
