package com.cinemaai.payment.dto.response;

import java.math.BigDecimal;

public record VNPayPaymentResponse(
        Long paymentId,
        String paymentUrl,
        String txnRef,
        BigDecimal amount,
        String provider
) {}
