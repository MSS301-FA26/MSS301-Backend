package com.cinemaai.catalog.dto.response.cinema;

import java.math.BigDecimal;

public record RoomPricingResponse(
        Long roomId,
        String roomName,
        BigDecimal standardPrice,
        BigDecimal vipPrice,
        BigDecimal couplePrice
) {
}
