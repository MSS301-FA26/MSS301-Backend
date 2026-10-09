package com.cinemaai.payment.dto.request;

import java.math.BigDecimal;

public record CreditWalletInternalRequest(
        Long userId,
        BigDecimal amount,
        Long bookingId,
        String bookingCode,
        String reason
) {}
