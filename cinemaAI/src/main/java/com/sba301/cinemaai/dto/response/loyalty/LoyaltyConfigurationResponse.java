package com.sba301.cinemaai.dto.response.loyalty;

import com.sba301.cinemaai.entity.LoyaltyConfiguration;
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
) {
    public static LoyaltyConfigurationResponse from(LoyaltyConfiguration config) {
        return new LoyaltyConfigurationResponse(
                config.getId(),
                config.getEarningRatePercent(),
                config.getRedemptionPoints(),
                config.getRedemptionValueVnd(),
                config.getExpiryMonth(),
                config.getExpiryDay(),
                config.getExpiryTime(),
                config.getLastExpiredAt(),
                config.getLastResetAt(),
                config.getLastResetSource()
        );
    }
}
