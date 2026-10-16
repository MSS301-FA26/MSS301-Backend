package com.cinemaai.payment.provider;

public record PayoutResult(
        boolean success,
        String providerTransactionId,
        String failureCode,
        String failureReason
) {
    public static PayoutResult success(String providerTransactionId) {
        return new PayoutResult(true, providerTransactionId, null, null);
    }

    public static PayoutResult failure(String failureCode, String failureReason) {
        return new PayoutResult(false, null, failureCode, failureReason);
    }
}
