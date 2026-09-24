package com.cinemaai.payment.controller;

import com.cinemaai.payment.dto.request.WithdrawalCreateRequest;
import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.dto.response.PageResponse;
import com.cinemaai.payment.dto.response.WalletResponse;
import com.cinemaai.payment.dto.response.WalletTransactionResponse;
import com.cinemaai.payment.dto.response.WithdrawalResponse;
import com.cinemaai.payment.security.AuthenticatedUser;
import com.cinemaai.payment.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/wallet", "/api/v1/wallets"})
@RequiredArgsConstructor
@Tag(name = "User - CineWallet", description = "Số dư ví, lịch sử giao dịch và yêu cầu rút tiền của khách hàng")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Lấy thông tin và số dư ví của người dùng")
    public ApiResponse<WalletResponse> getWallet(@AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(walletService.getWallet(userId), "Lấy thông tin ví thành công");
    }

    @GetMapping("/transactions")
    @Operation(summary = "Lấy lịch sử giao dịch ví của người dùng")
    public ApiResponse<PageResponse<WalletTransactionResponse>> getTransactions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(walletService.getTransactions(userId, page, size), "Lấy lịch sử giao dịch ví thành công");
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "Tạo yêu cầu rút tiền về tài khoản ngân hàng")
    public ApiResponse<WithdrawalResponse> createWithdrawal(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody WithdrawalCreateRequest request
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(
                walletService.createWithdrawalRequest(userId, request),
                "Yêu cầu rút tiền đã được tạo thành công"
        );
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Xem lịch sử các yêu cầu rút tiền của người dùng")
    public ApiResponse<PageResponse<WithdrawalResponse>> getMyWithdrawals(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(walletService.getMyWithdrawals(userId, page, size), "Lấy danh sách yêu cầu rút tiền thành công");
    }
}
