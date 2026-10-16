package com.cinemaai.payment.service;

import com.cinemaai.payment.dto.response.PaymentResponse;
import com.cinemaai.payment.dto.response.VNPayPaymentResponse;
import com.cinemaai.payment.entity.PaymentTransaction;
import java.util.List;
import java.util.Map;

public interface PaymentService {

    VNPayPaymentResponse createVnpayPayment(Long userId, Long bookingId, Long foodOrderId, String clientIp);

    PaymentResponse mockPayment(Long userId, Long bookingId, Long foodOrderId);

    PaymentResponse processWalletPayment(Long userId, Long bookingId, Long foodOrderId);

    Map<String, String> processVnpayIpn(Map<String, String> params);

    PaymentResponse getPaymentByBooking(Long bookingId);

    PaymentResponse getPaymentByFoodOrder(Long foodOrderId);

    PaymentResponse getPaymentById(Long paymentId);

    List<PaymentTransaction> getPaymentTransactions(Long paymentId);

    Map<String, Object> reconcilePayment(Long paymentId);
}
