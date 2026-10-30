package com.cinemaai.payment.controller;

import com.cinemaai.payment.dto.request.LoyaltyAddRequest;
import com.cinemaai.payment.dto.request.LoyaltyConfigurationRequest;
import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.dto.response.LoyaltyConfigurationResponse;
import com.cinemaai.payment.dto.response.LoyaltyReportResponse;
import com.cinemaai.payment.dto.response.LoyaltyResponse;
import com.cinemaai.payment.dto.response.LoyaltyTransactionResponse;
import com.cinemaai.payment.dto.response.PageResponse;
import com.cinemaai.payment.security.AuthenticatedUser;
import com.cinemaai.payment.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/loyalty")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@Tag(name = "Admin - Loyalty", description = "Quản lý điểm thưởng loyalty cho Admin và Manager")
public class AdminLoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Xem cấu hình tích điểm và quy đổi theo chi nhánh hoặc toàn hệ thống")
    public ApiResponse<LoyaltyConfigurationResponse> getConfiguration(
            @RequestParam(required = false) Long cinemaId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(loyaltyService.getAdminConfiguration(cinemaId, user));
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật cấu hình tích điểm và quy đổi theo chi nhánh hoặc toàn hệ thống")
    public ApiResponse<LoyaltyConfigurationResponse> updateConfiguration(
            @Valid @RequestBody LoyaltyConfigurationRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(loyaltyService.updateConfiguration(request, user), "Cập nhật cấu hình loyalty thành công");
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
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
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xem báo cáo tổng quan điểm thưởng")
    public ApiResponse<LoyaltyReportResponse> getReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ApiResponse.success(loyaltyService.getReport(from, to));
    }

    @PostMapping("/points/expire-all-active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cho hết hạn toàn bộ điểm thưởng hiện có",
            description = "Cho hết hạn số dư điểm thưởng hiện có của tất cả tài khoản có điểm dương, "
                    + "đồng thời ghi nhận giao dịch EXPIRE trong lịch sử điểm thưởng."
    )
    public ApiResponse<Integer> expireAllActivePoints() {
        int affected = loyaltyService.expireAllActivePoints("ADMIN");
        return ApiResponse.success(affected, "Đã cho hết hạn điểm thưởng của " + affected + " tài khoản");
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cộng điểm thưởng thủ công cho người dùng")
    public ApiResponse<LoyaltyResponse> addPoints(@Valid @RequestBody LoyaltyAddRequest request) {
        return ApiResponse.success(loyaltyService.addPoints(request), "Cộng điểm thưởng thành công");
    }

    @PostMapping("/{userId}/redeem")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trừ điểm thưởng thủ công cho người dùng")
    public ApiResponse<LoyaltyResponse> redeemPoints(
            @PathVariable Long userId,
            @RequestParam @Min(1) int points
    ) {
        return ApiResponse.success(loyaltyService.redeemPoints(userId, points), "Trừ điểm thưởng thành công");
    }
}
