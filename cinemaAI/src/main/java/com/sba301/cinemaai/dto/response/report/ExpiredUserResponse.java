package com.sba301.cinemaai.dto.response.report;

import java.math.BigDecimal;

public record ExpiredUserResponse(
        Long userId,
        String email,
        long expiredBookings,
        BigDecimal lostAmount
) {
}
