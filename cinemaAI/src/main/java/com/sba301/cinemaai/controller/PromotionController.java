package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.promotion.ValidateVoucherRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.promotion.PromotionResponse;
import com.sba301.cinemaai.dto.response.promotion.ValidateVoucherResponse;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.PromotionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions")
@Tag(name = "Promotions / Vouchers")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/active")
    public ApiResponse<List<PromotionResponse>> getActivePromotions() {
        return ApiResponse.success(promotionService.getActivePublicPromotions());
    }

    @PostMapping("/validate")
    public ApiResponse<ValidateVoucherResponse> validateVoucher(
            @Valid @RequestBody ValidateVoucherRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long userId = user != null ? user.id() : null;
        return ApiResponse.success(promotionService.validateVoucher(request, userId));
    }
}
