package com.cinemaai.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long id,
        BigDecimal balance,
        LocalDateTime createdAt
) {}
