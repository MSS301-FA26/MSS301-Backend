package com.cinemaai.catalog.dto.response.cinema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DaySurchargeResponse(
        Long cinemaId,
        BigDecimal weekendSurcharge,
        BigDecimal holidaySurcharge,
        BigDecimal nightSurcharge,
        LocalDateTime updatedAt
) {
    public DaySurchargeResponse(Long cinemaId, BigDecimal weekendSurcharge, BigDecimal holidaySurcharge, LocalDateTime updatedAt) {
        this(cinemaId, weekendSurcharge, holidaySurcharge, BigDecimal.ZERO, updatedAt);
    }
}
