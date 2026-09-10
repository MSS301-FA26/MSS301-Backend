package com.sba301.cinemaai.dto.request.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record WithdrawalCreateRequest(
        @NotNull(message = "Withdrawal amount is required")
        @DecimalMin(value = "10000", message = "Withdrawal amount must be at least 10000")
        BigDecimal amount,

        @NotBlank(message = "Bank name is required")
        @Size(max = 100, message = "Bank name must be at most 100 characters")
        String bankName,

        @NotBlank(message = "Account number is required")
        @Size(max = 50, message = "Account number must be at most 50 characters")
        String accountNumber,

        @NotBlank(message = "Account holder is required")
        @Size(max = 120, message = "Account holder must be at most 120 characters")
        String accountHolder,

        @Size(max = 20, message = "Wallet phone must be at most 20 characters")
        String walletPhone
) {
}
