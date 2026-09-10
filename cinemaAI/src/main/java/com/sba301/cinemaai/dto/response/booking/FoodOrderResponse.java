package com.sba301.cinemaai.dto.response.booking;

import com.sba301.cinemaai.enums.FoodOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FoodOrderResponse(
        Long id,
        String orderCode,
        Long bookingId,
        String bookingCode,
        FoodOrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime paidAt,
        LocalDateTime expiresAt,
        LocalDateTime pickedUpAt,
        String qrCode,
        LocalDateTime createdAt,
        boolean createdByStaff,
        List<BookingFoodResponse> items
) {
}
