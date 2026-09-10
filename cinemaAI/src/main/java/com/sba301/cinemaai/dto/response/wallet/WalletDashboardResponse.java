package com.sba301.cinemaai.dto.response.wallet;

import java.math.BigDecimal;
import java.util.List;

public record WalletDashboardResponse(
        BigDecimal totalWalletBalance,
        BigDecimal totalRefundedToWallet,
        BigDecimal totalWithdrawnAmount,
        BigDecimal pendingWithdrawalsAmount,
        long pendingWithdrawalsCount,
        long totalWallets,
        List<WalletTransactionResponse> recentTransactions
) {
}
