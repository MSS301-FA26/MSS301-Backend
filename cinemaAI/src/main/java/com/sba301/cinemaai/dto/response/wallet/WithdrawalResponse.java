package com.sba301.cinemaai.dto.response.wallet;

import com.sba301.cinemaai.entity.WithdrawalRequest;
import com.sba301.cinemaai.enums.WithdrawalStatus;
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
        // Admin-only fields
        Long userId,
        String userName,
        String userEmail
) {
    public static WithdrawalResponse from(WithdrawalRequest wr) {
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
                wr.getUser().getId(),
                wr.getUser().getFullName(),
                wr.getUser().getEmail()
        );
    }
}
