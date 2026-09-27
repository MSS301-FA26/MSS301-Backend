package com.cinemaai.booking.dto.response.report;

import java.math.BigDecimal;

public record ProviderRevenueResponse(
        String provider,
        BigDecimal revenue,
        long transactions
) {
}

