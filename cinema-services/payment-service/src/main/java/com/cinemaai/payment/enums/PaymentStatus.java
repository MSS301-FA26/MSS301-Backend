package com.cinemaai.payment.enums;

import java.util.Set;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    EXPIRED,
    CANCELLED,
    PARTIALLY_REFUNDED,
    REFUNDED;

    public boolean canTransitionTo(PaymentStatus target) {
        if (this == target) return true;
        return switch (this) {
            case PENDING -> Set.of(PROCESSING, SUCCESS, FAILED, EXPIRED, CANCELLED).contains(target);
            case PROCESSING -> Set.of(SUCCESS, FAILED, EXPIRED, CANCELLED).contains(target);
            case SUCCESS -> Set.of(PARTIALLY_REFUNDED, REFUNDED).contains(target);
            case PARTIALLY_REFUNDED -> Set.of(REFUNDED, PARTIALLY_REFUNDED).contains(target);
            case FAILED, EXPIRED, CANCELLED, REFUNDED -> false;
        };
    }
}
