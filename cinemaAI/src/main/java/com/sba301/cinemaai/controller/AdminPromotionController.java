package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.promotion.PromotionRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.promotion.PromotionResponse;
import com.sba301.cinemaai.dto.response.promotion.PromotionStatsResponse;
import com.sba301.cinemaai.service.PromotionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/promotions")
@Tag(name = "Admin Promotions / Vouchers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ApiResponse<List<PromotionResponse>> getAllPromotions(
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        return ApiResponse.success(promotionService.getAllPromotions(includeDeleted));
    }

    @GetMapping("/stats")
    public ApiResponse<PromotionStatsResponse> getPromotionStats() {
        return ApiResponse.success(promotionService.getPromotionStats());
    }

    @GetMapping("/{id}")
    public ApiResponse<PromotionResponse> getPromotionById(@PathVariable Long id) {
        return ApiResponse.success(promotionService.getPromotionById(id));
    }

    @PostMapping
    public ApiResponse<PromotionResponse> createPromotion(@Valid @RequestBody PromotionRequest request) {
        return ApiResponse.success(promotionService.createPromotion(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromotionResponse> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionRequest request
    ) {
        return ApiResponse.success(promotionService.updatePromotion(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PromotionResponse> togglePromotionStatus(@PathVariable Long id) {
        return ApiResponse.success(promotionService.togglePromotionStatus(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<PromotionResponse> restorePromotion(@PathVariable Long id) {
        return ApiResponse.success(promotionService.restorePromotion(id));
    }
}
