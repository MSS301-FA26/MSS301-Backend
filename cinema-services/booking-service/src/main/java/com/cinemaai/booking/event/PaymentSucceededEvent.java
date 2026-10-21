package com.cinemaai.booking.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentSucceededEvent(
        String eventId,
        Long paymentId,
        Long bookingId,
        Long foodOrderId,
        Long userId,
        BigDecimal amount,
        String provider,
        LocalDateTime paidAt
) {
    @JsonCreator
    public PaymentSucceededEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("paymentId") Long paymentId,
            @JsonProperty("bookingId") Long bookingId,
            @JsonProperty("foodOrderId") Long foodOrderId,
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("provider") String provider,
            @JsonProperty("paidAt") LocalDateTime paidAt
    ) {
        this(eventId, paymentId, bookingId, normalizeOrderId(foodOrderId, orderId), userId, amount, provider, paidAt);
    }

    public Long orderId() {
        return foodOrderId;
    }

    private static Long normalizeOrderId(Long foodOrderId, Long orderId) {
        if (foodOrderId != null && orderId != null && !foodOrderId.equals(orderId)) {
            throw new IllegalArgumentException("foodOrderId and orderId must identify the same order");
        }
        return foodOrderId != null ? foodOrderId : orderId;
    }
}
