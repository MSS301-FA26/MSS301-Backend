package com.sba301.cinemaai.dto.response.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long walletId,
        BigDecimal balance,
        LocalDateTime createdAt
) {
}
