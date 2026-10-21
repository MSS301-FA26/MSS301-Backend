package com.cinemaai.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CreatePaymentRequest(
        Long bookingId,
        Long foodOrderId
) {
    @JsonCreator
    public CreatePaymentRequest(
            @JsonProperty("bookingId") Long bookingId,
            @JsonProperty("foodOrderId") Long foodOrderId,
            @JsonProperty("orderId") Long orderId
    ) {
        this(bookingId, normalizeOrderId(foodOrderId, orderId));
    }

    @JsonProperty("orderId")
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
