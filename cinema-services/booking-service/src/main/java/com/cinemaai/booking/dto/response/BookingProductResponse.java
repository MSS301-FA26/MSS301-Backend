package com.cinemaai.booking.dto.response;

import java.math.BigDecimal;

public record BookingProductResponse(
        Long id,
        Long productId,
        boolean isCombo,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
