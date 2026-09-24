package com.cinemaai.payment.service;

import com.cinemaai.payment.dto.response.PaymentResponse;
import com.cinemaai.payment.dto.response.VNPayPaymentResponse;
import java.util.Map;

public interface PaymentService {

    VNPayPaymentResponse createVnpayPayment(Long userId, Long bookingId, Long foodOrderId, String clientIp);

    PaymentResponse mockPayment(Long userId, Long bookingId, Long foodOrderId);

    Map<String, String> processVnpayIpn(Map<String, String> params);

    PaymentResponse getPaymentByBooking(Long bookingId);

    PaymentResponse getPaymentById(Long paymentId);
}
