package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.config.PaymentRedirectProperties;
import com.sba301.cinemaai.dto.response.payment.PaymentResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment APIs")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRedirectProperties paymentRedirectProperties;

    @PostMapping("/vnpay/create")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create VNPAY payment URL", description = "Creates a VNPAY payment for a HOLDING or PENDING_PAYMENT booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> createVnpayPayment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam Long bookingId,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);
        PaymentResponse response = paymentService.createVnpayPayment(user.email(), bookingId, clientIp);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment URL created"));
    }

    @GetMapping("/vnpay/return")
    @Operation(summary = "VNPAY return URL handler", description = "Called by VNPAY after user completes payment")
    public ResponseEntity<Void> vnpayReturn(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        paymentService.handleVnpayReturn(params);
        String queryString = request.getQueryString();
        String successRedirectUrl = resolveSuccessRedirectUrl();
        String redirectUrl = successRedirectUrl
                + (queryString != null ? "?" + queryString : "");
        return ResponseEntity.status(302).header("Location", redirectUrl).build();
    }

    @GetMapping("/vnpay/null")
    @Operation(summary = "VNPAY legacy null return URL handler", description = "Handles old callbacks generated before redirect URL was configured")
    public ResponseEntity<Void> vnpayNullReturn(HttpServletRequest request) {
        return vnpayReturn(request);
    }

    @GetMapping("/vnpay/ipn")
    @Operation(summary = "VNPAY IPN webhook", description = "Called by VNPAY server to confirm payment status")
    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        String result = paymentService.handleVnpayIpn(params);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/mock")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Mock payment (dev only)", description = "Marks booking as paid without real payment gateway")
    public ResponseEntity<ApiResponse<PaymentResponse>> mockPayment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam Long bookingId
    ) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.mockPayment(user.email(), bookingId), "Payment confirmed"));
    }

    @PostMapping("/food-orders/{foodOrderId}/vnpay/create")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create VNPAY payment URL for a food order")
    public ResponseEntity<ApiResponse<PaymentResponse>> createVnpayFoodOrderPayment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long foodOrderId,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);
        PaymentResponse response = paymentService.createVnpayFoodOrderPayment(user.email(), foodOrderId, clientIp);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment URL created"));
    }

    @PostMapping("/food-orders/{foodOrderId}/mock")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Mock payment for a food order (dev only)")
    public ResponseEntity<ApiResponse<PaymentResponse>> mockFoodOrderPayment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long foodOrderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.mockFoodOrderPayment(user.email(), foodOrderId), "Payment confirmed"));
    }

    @GetMapping("/booking/{bookingId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get payment by booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByBooking(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getByBooking(user.email(), bookingId)));
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveSuccessRedirectUrl() {
        String configuredUrl = paymentRedirectProperties.getSuccessRedirectUrl();
        return (configuredUrl == null || configuredUrl.isBlank() || "null".equalsIgnoreCase(configuredUrl))
                ? "http://localhost:3000/payment-callback"
                : configuredUrl;
    }
}
