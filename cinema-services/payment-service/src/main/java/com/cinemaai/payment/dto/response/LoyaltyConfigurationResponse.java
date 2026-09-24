package com.cinemaai.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoyaltyConfigurationResponse(
        Long id,
        BigDecimal earningRatePercent,
        int redemptionPoints,
        BigDecimal redemptionValueVnd,
        int expiryMonth,
        int expiryDay,
        String expiryTime,
        LocalDate lastExpiredAt,
        LocalDateTime lastResetAt,
        String lastResetSource
) {}
