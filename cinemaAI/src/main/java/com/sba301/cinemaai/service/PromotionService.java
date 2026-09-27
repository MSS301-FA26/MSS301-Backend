package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.promotion.PromotionRequest;
import com.sba301.cinemaai.dto.request.promotion.ValidateVoucherRequest;
import com.sba301.cinemaai.dto.response.promotion.PromotionResponse;
import com.sba301.cinemaai.dto.response.promotion.PromotionStatsResponse;
import com.sba301.cinemaai.dto.response.promotion.ValidateVoucherResponse;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionService {

    List<PromotionResponse> getAllPromotions(boolean includeDeleted);

    List<PromotionResponse> getActivePublicPromotions();

    PromotionResponse getPromotionById(Long id);

    PromotionResponse createPromotion(PromotionRequest request);

    PromotionResponse updatePromotion(Long id, PromotionRequest request);

    PromotionResponse togglePromotionStatus(Long id);

    void deletePromotion(Long id);

    PromotionResponse restorePromotion(Long id);

    ValidateVoucherResponse validateVoucher(ValidateVoucherRequest request, Long userId);

    PromotionStatsResponse getPromotionStats();

    void recordPromotionUsage(String code, Long userId, Long bookingId, Long foodOrderId, BigDecimal discountAmount);
}
