package com.cinemaai.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoyaltyConfigurationResponse(
        Long id,
        Long cinemaId,
        String cinemaName,
        BigDecimal earningRatePercent,
        BigDecimal redemptionRatePercent,
        int redemptionPoints,
        BigDecimal redemptionValueVnd,
        BigDecimal maxRedemptionPercent,
        int expiryMonth,
        int expiryDay,
        String expiryTime,
        LocalDate lastExpiredAt,
        LocalDateTime lastResetAt,
        String lastResetSource
) {}
