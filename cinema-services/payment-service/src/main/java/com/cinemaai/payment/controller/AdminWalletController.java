package com.cinemaai.payment.controller;

import com.cinemaai.payment.dto.request.WithdrawalProcessRequest;
import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.dto.response.PageResponse;
import com.cinemaai.payment.dto.response.WalletDashboardResponse;
import com.cinemaai.payment.dto.response.WithdrawalResponse;
import com.cinemaai.payment.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/admin/wallet", "/api/v1/admin/wallets"})
@RequiredArgsConstructor
@Tag(name = "Admin / Staff - CineWallet", description = "Quản lý ví và duyệt yêu cầu rút tiền cho Staff/Admin")
public class AdminWalletController {

    private final WalletService walletService;

    @GetMapping("/dashboard")
    @Operation(summary = "Lấy dữ liệu Dashboard CineWallet cho Staff / Admin")
    public ApiResponse<WalletDashboardResponse> getDashboard() {
        return ApiResponse.success(walletService.getDashboard(), "Lấy dữ liệu Dashboard ví thành công");
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Lấy danh sách yêu cầu rút tiền có phân trang và lọc theo trạng thái")
    public ApiResponse<PageResponse<WithdrawalResponse>> getAllWithdrawals(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(walletService.getAllWithdrawals(status, page, size), "Lấy danh sách yêu cầu rút tiền thành công");
    }

    @PostMapping("/withdrawals/{id}/approve")
    @Operation(summary = "Duyệt yêu cầu rút tiền (Xác nhận đã chuyển khoản ngân hàng)")
    public ApiResponse<WithdrawalResponse> approveWithdrawal(
            @PathVariable Long id,
            @RequestBody(required = false) WithdrawalProcessRequest request
    ) {
        return ApiResponse.success(
                walletService.approveWithdrawal(id, request),
                "Đã duyệt yêu cầu rút tiền thành công"
        );
    }

    @PostMapping("/withdrawals/{id}/reject")
    @Operation(summary = "Từ chối yêu cầu rút tiền và hoàn lại số dư vào ví")
    public ApiResponse<WithdrawalResponse> rejectWithdrawal(
            @PathVariable Long id,
            @RequestBody(required = false) WithdrawalProcessRequest request
    ) {
        return ApiResponse.success(
                walletService.rejectWithdrawal(id, request),
                "Đã từ chối yêu cầu rút tiền và hoàn tiền về ví người dùng"
        );
    }
}
