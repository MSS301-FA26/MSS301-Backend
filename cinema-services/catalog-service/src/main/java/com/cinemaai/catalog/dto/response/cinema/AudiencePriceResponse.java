package com.cinemaai.catalog.dto.response.cinema;

import com.cinemaai.catalog.enums.AudienceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AudiencePriceResponse(
        Long id,
        Long cinemaId,
        AudienceType audienceType,
        BigDecimal additionalPrice,
        BigDecimal standardPrice,
        BigDecimal vipPrice,
        BigDecimal couplePrice,
        LocalDateTime updatedAt
) {
}
