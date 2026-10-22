package com.cinemaai.payment.controller;

import com.cinemaai.payment.dto.request.CreditWalletInternalRequest;
import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.dto.response.WalletResponse;
import com.cinemaai.payment.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Internal Wallet API", description = "API noi bo cong tien hoan vao CineWallet")
public class InternalWalletController {

    private final WalletService walletService;

    @Operation(summary = "Cong tien hoan ve vao CineWallet cua khach hang (Internal)")
    @PostMapping("/credit")
    public ApiResponse<WalletResponse> creditWalletInternal(@RequestBody CreditWalletInternalRequest request) {
        return ApiResponse.success(
                walletService.creditWalletInternal(request),
                "Cong tien vao CineWallet thanh cong"
        );
    }
}
