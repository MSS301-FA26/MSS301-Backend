package com.cinemaai.payment.dto.request;

public record CreatePaymentRequest(
        Long bookingId,
        Long foodOrderId
) {}
