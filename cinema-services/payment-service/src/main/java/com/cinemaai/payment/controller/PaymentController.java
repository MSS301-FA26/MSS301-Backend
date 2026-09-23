package com.cinemaai.payment.controller;

import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.dto.response.PaymentResponse;
import com.cinemaai.payment.dto.response.VNPayPaymentResponse;
import com.cinemaai.payment.security.AuthenticatedUser;
import com.cinemaai.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Core", description = "Quản lý giao dịch thanh toán và VNPay Gateway")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Tạo URL thanh toán VNPay Sandbox")
    @PostMapping("/vnpay/create")
    public ApiResponse<VNPayPaymentResponse> createVnpayPayment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) com.cinemaai.payment.dto.request.CreatePaymentRequest body,
            @RequestParam(required = false) Long bookingId,
            @RequestParam(required = false) Long foodOrderId,
            HttpServletRequest request
    ) {
        Long userId = user != null ? user.id() : 1L;
        Long targetBookingId = body != null && body.bookingId() != null ? body.bookingId() : bookingId;
        Long targetFoodOrderId = body != null && body.foodOrderId() != null ? body.foodOrderId() : foodOrderId;

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }
        VNPayPaymentResponse response = paymentService.createVnpayPayment(userId, targetBookingId, targetFoodOrderId, clientIp);
        return ApiResponse.success(response, "Khởi tạo thanh toán VNPay thành công");
    }

    @Operation(summary = "Thanh toán giả lập phục vụ Test/Demo (Mock Payment)")
    @PostMapping("/mock")
    public ApiResponse<PaymentResponse> mockPayment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) com.cinemaai.payment.dto.request.CreatePaymentRequest body,
            @RequestParam(required = false) Long bookingId,
            @RequestParam(required = false) Long foodOrderId
    ) {
        Long userId = user != null ? user.id() : 1L;
        Long targetBookingId = body != null && body.bookingId() != null ? body.bookingId() : bookingId;
        Long targetFoodOrderId = body != null && body.foodOrderId() != null ? body.foodOrderId() : foodOrderId;

        PaymentResponse response = paymentService.mockPayment(userId, targetBookingId, targetFoodOrderId);
        return ApiResponse.success(response, "Thanh toán giả lập thành công");
    }

    @Operation(summary = "Webhook IPN xử lý kết quả từ cổng VNPay (Idempotent)")
    @RequestMapping(value = "/vnpay/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> allParams) {
        return paymentService.processVnpayIpn(allParams);
    }

    @Operation(summary = "Lấy thông tin thanh toán theo mã đặt vé")
    @GetMapping("/booking/{bookingId}")
    public ApiResponse<PaymentResponse> getPaymentByBooking(@PathVariable Long bookingId) {
        return ApiResponse.success(paymentService.getPaymentByBooking(bookingId));
    }

    @Operation(summary = "Lấy thông tin thanh toán theo paymentId")
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPaymentById(@PathVariable Long paymentId) {
        return ApiResponse.success(paymentService.getPaymentById(paymentId));
    }
}
