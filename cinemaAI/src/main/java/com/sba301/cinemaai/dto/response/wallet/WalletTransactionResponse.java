package com.sba301.cinemaai.dto.response.wallet;

import com.sba301.cinemaai.entity.WalletTransaction;
import com.sba301.cinemaai.enums.WalletTransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionResponse(
        Long id,
        WalletTransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String referenceCode,
        String note,
        Long bookingId,
        String bookingCode,
        LocalDateTime createdAt
) {
    public static WalletTransactionResponse from(WalletTransaction tx) {
        return new WalletTransactionResponse(
                tx.getId(),
                tx.getType(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getReferenceCode(),
                tx.getNote(),
                tx.getBooking() == null ? null : tx.getBooking().getId(),
                tx.getBooking() == null ? null : tx.getBooking().getBookingCode(),
                tx.getCreatedAt()
        );
    }
}
