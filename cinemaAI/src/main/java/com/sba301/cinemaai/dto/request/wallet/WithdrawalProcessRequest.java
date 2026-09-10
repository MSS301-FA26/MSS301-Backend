package com.sba301.cinemaai.dto.request.wallet;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WithdrawalProcessRequest(
        @Pattern(regexp = "MANUAL_BANK_TRANSFER|BANK_TRANSFER|CASH|E_WALLET", message = "Process method is invalid")
        String method,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}
