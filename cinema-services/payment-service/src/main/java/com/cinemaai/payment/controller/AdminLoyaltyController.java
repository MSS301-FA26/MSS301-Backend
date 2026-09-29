package com.cinemaai.payment.controller;

import com.cinemaai.payment.dto.request.LoyaltyAddRequest;
import com.cinemaai.payment.dto.request.LoyaltyConfigurationRequest;
import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.dto.response.LoyaltyConfigurationResponse;
import com.cinemaai.payment.dto.response.LoyaltyReportResponse;
import com.cinemaai.payment.dto.response.LoyaltyResponse;
import com.cinemaai.payment.dto.response.LoyaltyTransactionResponse;
import com.cinemaai.payment.dto.response.PageResponse;
import com.cinemaai.payment.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/loyalty")
@RequiredArgsConstructor
@Tag(name = "Admin - Loyalty", description = "Quản lý điểm thưởng loyalty cho Admin")
public class AdminLoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/config")
    @Operation(summary = "Xem cấu hình tích điểm và quy đổi")
    public ApiResponse<LoyaltyConfigurationResponse> getConfiguration() {
        return ApiResponse.success(loyaltyService.getConfiguration());
    }

    @PutMapping("/config")
    @Operation(summary = "Cập nhật cấu hình tích điểm và quy đổi")
    public ApiResponse<LoyaltyConfigurationResponse> updateConfiguration(
            @Valid @RequestBody LoyaltyConfigurationRequest request
    ) {
        return ApiResponse.success(loyaltyService.updateConfiguration(request), "Cập nhật cấu hình loyalty thành công");
    }

    @GetMapping("/transactions")
    @Operation(summary = "Tìm kiếm lịch sử giao dịch điểm thưởng")
    public ApiResponse<PageResponse<LoyaltyTransactionResponse>> searchTransactions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(loyaltyService.searchTransactions(keyword, from, to, page, size));
    }

    @GetMapping("/report")
    @Operation(summary = "Xem báo cáo tổng quan điểm thưởng")
    public ApiResponse<LoyaltyReportResponse> getReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ApiResponse.success(loyaltyService.getReport(from, to));
    }

    @PostMapping("/expire-now")
    @Operation(summary = "Reset/Hết hạn tất cả điểm thưởng")
    public ApiResponse<Integer> expireNow() {
        int affected = loyaltyService.expireAllActivePoints("ADMIN");
        return ApiResponse.success(affected, "Đã hết hạn điểm cho " + affected + " tài khoản");
    }

    @PostMapping("/add")
    @Operation(summary = "Cộng điểm thưởng thủ công cho người dùng")
    public ApiResponse<LoyaltyResponse> addPoints(@Valid @RequestBody LoyaltyAddRequest request) {
        return ApiResponse.success(loyaltyService.addPoints(request), "Cộng điểm thưởng thành công");
    }

    @PostMapping("/{userId}/redeem")
    @Operation(summary = "Trừ điểm thưởng thủ công cho người dùng")
    public ApiResponse<LoyaltyResponse> redeemPoints(
            @PathVariable Long userId,
            @RequestParam @Min(1) int points
    ) {
        return ApiResponse.success(loyaltyService.redeemPoints(userId, points), "Trừ điểm thưởng thành công");
    }
}
