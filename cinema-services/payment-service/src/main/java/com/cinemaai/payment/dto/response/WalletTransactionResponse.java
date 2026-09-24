package com.cinemaai.payment.dto.response;

import com.cinemaai.payment.entity.WalletTransaction;
import com.cinemaai.payment.enums.WalletTransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionResponse(
        Long id,
        Long walletId,
        Long userId,
        Long bookingId,
        WalletTransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String referenceCode,
        String description,
        LocalDateTime createdAt
) {
    public static WalletTransactionResponse from(WalletTransaction tx) {
        if (tx == null) return null;
        return new WalletTransactionResponse(
                tx.getId(),
                tx.getWallet() != null ? tx.getWallet().getId() : null,
                tx.getUserId(),
                tx.getBookingId(),
                tx.getType(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getReferenceCode(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}
