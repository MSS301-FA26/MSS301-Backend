package com.cinemaai.payment.mapper;

import com.cinemaai.payment.dto.response.PaymentResponse;
import com.cinemaai.payment.entity.Payment;

public class PaymentMapper {

    public static PaymentResponse toResponse(Payment payment) {
        if (payment == null) return null;
        return new PaymentResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getFoodOrderId(),
                payment.getUserId(),
                payment.getProvider(),
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaymentUrl(),
                payment.getPaymentAccountLabel(),
                payment.getPaidAt(),
                payment.getRefundAmount(),
                payment.getRefundedAt(),
                payment.getCreatedAt()
        );
    }
}
