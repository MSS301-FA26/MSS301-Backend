package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.wallet.WithdrawalCreateRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.wallet.WalletResponse;
import com.sba301.cinemaai.dto.response.wallet.WalletTransactionResponse;
import com.sba301.cinemaai.dto.response.wallet.WithdrawalResponse;
import com.sba301.cinemaai.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "User - CineWallet", description = "User wallet balance, transactions and withdrawal requests")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Get wallet info", description = "Returns current wallet balance. Auto-creates wallet if not exists.")
    public ApiResponse<WalletResponse> getWallet(Principal principal) {
        return ApiResponse.success(walletService.getWallet(principal.getName()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get wallet transaction history", description = "Paginated list of wallet transactions (refunds, withdrawals).")
    public ApiResponse<PageResponse<WalletTransactionResponse>> getTransactions(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(walletService.getTransactions(principal.getName(), page, size));
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "Create withdrawal request", description = "Request withdrawal from CineWallet to bank account. Staff will process manually.")
    public ApiResponse<WithdrawalResponse> createWithdrawal(
            Principal principal,
            @Valid @RequestBody WithdrawalCreateRequest request
    ) {
        return ApiResponse.success(
                walletService.createWithdrawalRequest(principal.getName(), request),
                "Yêu cầu rút tiền đã được tạo thành công"
        );
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Get my withdrawal history", description = "Paginated list of user's withdrawal requests.")
    public ApiResponse<PageResponse<WithdrawalResponse>> getMyWithdrawals(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(walletService.getMyWithdrawals(principal.getName(), page, size));
    }
}
