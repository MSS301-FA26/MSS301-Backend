package com.cinemaai.booking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderCode,
        String foodOrderCode,
        Long bookingId,
        String bookingCode,
        String status,
        BigDecimal subtotal,
        BigDecimal totalAmount,
        LocalDateTime paidAt,
        LocalDateTime expiresAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        String qrCode,
        Long cinemaId,
        String cinemaName,
        String cinemaAddress,
        List<OrderItemResponse> items
) {
    public OrderResponse(
            Long id,
            String orderCode,
            String foodOrderCode,
            Long bookingId,
            String bookingCode,
            String status,
            BigDecimal subtotal,
            BigDecimal totalAmount,
            LocalDateTime paidAt,
            LocalDateTime expiresAt,
            LocalDateTime cancelledAt,
            LocalDateTime createdAt,
            String qrCode,
            List<OrderItemResponse> items
    ) {
        this(id, orderCode, foodOrderCode, bookingId, bookingCode, status, subtotal, totalAmount, paidAt, expiresAt, cancelledAt, createdAt, qrCode, null, null, null, items);
    }

    public OrderResponse(
            Long id,
            String orderCode,
            String foodOrderCode,
            Long bookingId,
            String bookingCode,
            String status,
            BigDecimal subtotal,
            BigDecimal totalAmount,
            LocalDateTime paidAt,
            LocalDateTime expiresAt,
            LocalDateTime cancelledAt,
            LocalDateTime createdAt,
            List<OrderItemResponse> items
    ) {
        this(id, orderCode, foodOrderCode, bookingId, bookingCode, status, subtotal, totalAmount, paidAt, expiresAt, cancelledAt, createdAt, null, null, null, null, items);
    }
}
