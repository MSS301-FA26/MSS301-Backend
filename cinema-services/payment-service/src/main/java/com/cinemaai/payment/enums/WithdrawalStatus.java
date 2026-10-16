package com.cinemaai.payment.enums;

public enum WithdrawalStatus {
    // Standard lifecycle per payment.md Section XVII
    REQUESTED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,

    // Legacy/Admin compatibility aliases
    PENDING,
    PAID,
    REJECTED;

    public boolean isTerminal() {
        return this == COMPLETED || this == PAID || this == FAILED || this == REJECTED || this == CANCELLED;
    }
}
