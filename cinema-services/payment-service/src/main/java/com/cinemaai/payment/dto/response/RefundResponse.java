package com.cinemaai.payment.dto.response;

import com.cinemaai.payment.entity.Refund;
import com.cinemaai.payment.enums.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse(
        Long id,
        String refundCode,
        Long paymentId,
        Long bookingId,
        Long foodOrderId,
        Long userId,
        BigDecimal amount,
        BigDecimal remainingRefundableAmount,
        String reason,
        RefundStatus status,
        Long refundBatchId,
        String failureReason,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
    public static RefundResponse from(Refund r) {
        if (r == null) return null;
        return new RefundResponse(
                r.getId(),
                r.getRefundCode(),
                r.getPaymentId(),
                r.getBookingId(),
                r.getFoodOrderId(),
                r.getUserId(),
                r.getAmount(),
                r.getRemainingRefundableAmount(),
                r.getReason(),
                r.getStatus(),
                r.getRefundBatch() != null ? r.getRefundBatch().getId() : null,
                r.getFailureReason(),
                r.getRequestedAt(),
                r.getCompletedAt(),
                r.getCreatedAt()
        );
    }
}
