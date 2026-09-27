package com.cinemaai.payment.dto.response;

import com.cinemaai.payment.entity.WithdrawalRequest;
import com.cinemaai.payment.enums.WithdrawalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WithdrawalResponse(
        Long id,
        BigDecimal amount,
        String bankName,
        String accountNumber,
        String accountHolder,
        String walletPhone,
        WithdrawalStatus status,
        String processedMethod,
        String processedNote,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        Long userId,
        String userName,
        String userEmail
) {
    public static WithdrawalResponse from(WithdrawalRequest wr) {
        if (wr == null) return null;
        return new WithdrawalResponse(
                wr.getId(),
                wr.getAmount(),
                wr.getBankName(),
                wr.getAccountNumber(),
                wr.getAccountHolder(),
                wr.getWalletPhone(),
                wr.getStatus(),
                wr.getProcessedMethod(),
                wr.getProcessedNote(),
                wr.getProcessedAt(),
                wr.getCreatedAt(),
                wr.getUserId(),
                "User #" + wr.getUserId(),
                wr.getWalletPhone()
        );
    }
}
