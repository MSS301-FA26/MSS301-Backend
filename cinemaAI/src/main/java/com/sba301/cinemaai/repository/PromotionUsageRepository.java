package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {

    long countByPromotionId(Long promotionId);

    long countByPromotionIdAndUserId(Long promotionId, Long userId);

    List<PromotionUsage> findByPromotionIdOrderByUsedAtDesc(Long promotionId);

    List<PromotionUsage> findByUserIdOrderByUsedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(pu.discountAmount), 0) FROM PromotionUsage pu")
    BigDecimal sumTotalDiscountGiven();

    @Query("SELECT COUNT(pu) FROM PromotionUsage pu")
    long countTotalUsages();
}
