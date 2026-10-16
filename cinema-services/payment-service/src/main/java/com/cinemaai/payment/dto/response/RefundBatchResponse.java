package com.cinemaai.payment.dto.response;

import com.cinemaai.payment.entity.RefundBatch;
import com.cinemaai.payment.enums.RefundBatchStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundBatchResponse(
        Long id,
        String batchCode,
        Long cinemaId,
        Long showtimeId,
        String reason,
        int totalBookings,
        BigDecimal totalAmount,
        RefundBatchStatus status,
        Long createdBy,
        Long approvedBy,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
    public static RefundBatchResponse from(RefundBatch b) {
        if (b == null) return null;
        return new RefundBatchResponse(
                b.getId(),
                b.getBatchCode(),
                b.getCinemaId(),
                b.getShowtimeId(),
                b.getReason(),
                b.getTotalBookings() != null ? b.getTotalBookings() : 0,
                b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO,
                b.getStatus(),
                b.getCreatedBy(),
                b.getApprovedBy(),
                b.getCompletedAt(),
                b.getCreatedAt()
        );
    }
}
