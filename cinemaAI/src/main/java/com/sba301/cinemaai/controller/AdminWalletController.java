package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.wallet.WithdrawalProcessRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.wallet.WalletDashboardResponse;
import com.sba301.cinemaai.dto.response.wallet.WithdrawalResponse;
import com.sba301.cinemaai.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/wallet")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - CineWallet", description = "Admin dashboard and withdrawal management for CineWallet")
public class AdminWalletController {

    private final WalletService walletService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get CineWallet dashboard", description = "Aggregated wallet statistics: total balance, refunds, withdrawals, pending requests.")
    public ApiResponse<WalletDashboardResponse> getDashboard() {
        return ApiResponse.success(walletService.getDashboard());
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "List all withdrawal requests", description = "Paginated list filtered by status (PENDING, PAID, REJECTED). Empty status returns all.")
    public ApiResponse<PageResponse<WithdrawalResponse>> getAllWithdrawals(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(walletService.getAllWithdrawals(status, page, size));
    }

    @PostMapping("/withdrawals/{id}/approve")
    @Operation(summary = "Approve withdrawal request", description = "Mark withdrawal as PAID after staff has completed manual bank transfer.")
    public ApiResponse<WithdrawalResponse> approveWithdrawal(
            @PathVariable Long id,
            @Valid @RequestBody WithdrawalProcessRequest request
    ) {
        return ApiResponse.success(
                walletService.approveWithdrawal(id, request),
                "Đã duyệt yêu cầu rút tiền"
        );
    }

    @PostMapping("/withdrawals/{id}/reject")
    @Operation(summary = "Reject withdrawal request", description = "Reject withdrawal and refund the held amount back to the user's wallet.")
    public ApiResponse<WithdrawalResponse> rejectWithdrawal(
            @PathVariable Long id,
            @Valid @RequestBody WithdrawalProcessRequest request
    ) {
        return ApiResponse.success(
                walletService.rejectWithdrawal(id, request),
                "Đã từ chối yêu cầu rút tiền và hoàn lại số dư"
        );
    }
}
