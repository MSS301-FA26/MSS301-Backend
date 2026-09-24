package com.cinemaai.payment.controller;

import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.dto.response.LoyaltyConfigurationResponse;
import com.cinemaai.payment.dto.response.LoyaltyResponse;
import com.cinemaai.payment.security.AuthenticatedUser;
import com.cinemaai.payment.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
@Tag(name = "Loyalty Core", description = "Quản lý điểm thưởng và chính sách thành viên")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @Operation(summary = "Xem điểm thưởng của tài khoản hiện tại")
    @GetMapping("/me")
    public ApiResponse<LoyaltyResponse> getMyPoints(@AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = user != null ? user.id() : null;
        String email = user != null ? user.email() : null;
        return ApiResponse.success(loyaltyService.getMyPoints(userId, email));
    }

    @Operation(summary = "Xem cấu hình quy đổi điểm thưởng")
    @GetMapping("/config")
    public ApiResponse<LoyaltyConfigurationResponse> getConfiguration() {
        return ApiResponse.success(loyaltyService.getConfiguration());
    }

    @Operation(summary = "Khách hàng đổi điểm thưởng lấy voucher giảm giá")
    @PostMapping("/me/redeem")
    public ApiResponse<LoyaltyResponse> redeemMyPoints(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam int points
    ) {
        Long userId = user != null ? user.id() : null;
        String email = user != null ? user.email() : null;
        return ApiResponse.success(loyaltyService.redeemMyPoints(userId, email, points), "Đổi điểm thành công");
    }
}
