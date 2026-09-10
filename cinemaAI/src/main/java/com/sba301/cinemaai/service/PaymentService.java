package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.response.payment.PaymentResponse;
import java.util.Map;

public interface PaymentService {

        public PaymentResponse createVnpayPayment(String email, Long bookingId, String clientIp);

        public String handleVnpayReturn(Map<String, String> params);

        public String handleVnpayIpn(Map<String, String> params);

        public PaymentResponse mockPayment(String email, Long bookingId);

        public PaymentResponse createVnpayFoodOrderPayment(String email, Long foodOrderId, String clientIp);

        public PaymentResponse mockFoodOrderPayment(String email, Long foodOrderId);

        public PaymentResponse getByBooking(String email, Long bookingId);
}
