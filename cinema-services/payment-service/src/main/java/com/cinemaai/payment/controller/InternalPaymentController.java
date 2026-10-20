package com.cinemaai.payment.controller;

import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Internal Payment API", description = "Internal communication between microservices")
public class InternalPaymentController {

    private final PaymentRepository paymentRepository;

    @Operation(summary = "Kiểm tra trạng thái thanh toán đơn bắp nước (Internal)")
    @GetMapping("/food-orders/{foodOrderId}/status")
    public ApiResponse<Map<String, Object>> getFoodOrderPaymentStatus(@PathVariable Long foodOrderId) {
        Payment payment = paymentRepository.findByFoodOrderId(foodOrderId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            return ApiResponse.success(Map.of(
                    "paid", true,
                    "paymentId", payment.getId(),
                    "transactionId", payment.getTransactionId() != null ? payment.getTransactionId() : ""
            ));
        }
        return ApiResponse.success(Map.of("paid", false));
    }
}
