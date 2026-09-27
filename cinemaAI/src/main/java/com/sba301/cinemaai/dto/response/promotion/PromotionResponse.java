package com.sba301.cinemaai.dto.response.promotion;

import com.sba301.cinemaai.enums.DiscountType;
import com.sba301.cinemaai.enums.PromotionStatus;
import com.sba301.cinemaai.enums.PromotionTarget;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionResponse(
        Long id,
        String code,
        String name,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderValue,
        BigDecimal maxDiscountAmount,
        Integer usageLimit,
        int usedCount,
        int userUsageLimit,
        PromotionTarget applicableTarget,
        LocalDateTime startDate,
        LocalDateTime endDate,
        PromotionStatus status,
        boolean isCurrentlyValid,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
