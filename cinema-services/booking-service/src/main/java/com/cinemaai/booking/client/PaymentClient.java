package com.cinemaai.booking.client;

import com.cinemaai.booking.dto.response.ApiResponse;
import java.math.BigDecimal;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PaymentClient {

    private final RestClient restClient;
    private final String internalSecret;

    public PaymentClient(
            @Value("${payment.service.url:http://localhost:8084}") String paymentServiceUrl,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.internalSecret = internalSecret;
        this.restClient = RestClient.builder().baseUrl(paymentServiceUrl).build();
    }

    public boolean checkFoodOrderPaymentSuccess(Long foodOrderId) {
        try {
            ApiResponse<Map<String, Object>> res = restClient.get()
                    .uri("/internal/v1/payments/food-orders/{foodOrderId}/status", foodOrderId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});

            if (res != null && res.data() != null) {
                Object paid = res.data().get("paid");
                return Boolean.TRUE.equals(paid);
            }
        } catch (Exception ex) {
            log.warn("Failed to check food order payment status from payment service: {}", ex.getMessage());
        }
        return false;
    }

    public BigDecimal creditWalletForRefund(Long userId, BigDecimal amount, Long bookingId, String bookingCode, String reason) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid wallet credit parameters: userId={}, amount={}", userId, amount);
            return null;
        }
        try {
            Map<String, Object> body = Map.of(
                    "userId", userId,
                    "amount", amount,
                    "bookingId", bookingId != null ? bookingId : 0L,
                    "bookingCode", bookingCode != null ? bookingCode : "",
                    "reason", reason != null ? reason : "Hoan tien ve"
            );

            ApiResponse<Map<String, Object>> response = restClient.post()
                    .uri("/internal/v1/wallets/credit")
                    .header("X-Internal-Service-Secret", internalSecret)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});

            if (response != null && response.success() && response.data() != null) {
                Object balObj = response.data().get("balance");
                BigDecimal newBalance = null;
                if (balObj instanceof Number num) {
                    newBalance = BigDecimal.valueOf(num.doubleValue());
                } else if (balObj != null) {
                    newBalance = new BigDecimal(balObj.toString());
                }
                log.info("Successfully credited {} to CineWallet of userId {} for booking {}, new balance: {}",
                        amount, userId, bookingCode, newBalance);
                return newBalance != null ? newBalance : BigDecimal.ZERO;
            }
        } catch (Exception ex) {
            log.error("Failed to credit CineWallet for userId {} booking {}: {}", userId, bookingCode, ex.getMessage());
        }
        return null;
    }

    public void awardLoyaltyPoints(Long userId, Long bookingId, String bookingCode, BigDecimal amount, Integer redeemedPoints) {
        if (userId == null) {
            return;
        }
        try {
            Map<String, Object> body = Map.of(
                    "userId", userId,
                    "bookingId", bookingId != null ? bookingId : 0L,
                    "bookingCode", bookingCode != null ? bookingCode : "",
                    "amount", amount != null ? amount : BigDecimal.ZERO,
                    "redeemedPoints", redeemedPoints != null ? redeemedPoints : 0
            );

            restClient.post()
                    .uri("/internal/v1/loyalty/award")
                    .header("X-Internal-Service-Secret", internalSecret)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully requested loyalty points award for userId {} booking {}", userId, bookingCode);
        } catch (Exception ex) {
            log.warn("Failed to award loyalty points for userId {} booking {}: {}", userId, bookingCode, ex.getMessage());
        }
    }

    public void refundLoyaltyPoints(Long userId, Long bookingId, String bookingCode, BigDecimal amount, Integer redeemedPoints, String reason) {
        if (userId == null) {
            return;
        }
        try {
            Map<String, Object> body = Map.of(
                    "userId", userId,
                    "bookingId", bookingId != null ? bookingId : 0L,
                    "bookingCode", bookingCode != null ? bookingCode : "",
                    "amount", amount != null ? amount : BigDecimal.ZERO,
                    "redeemedPoints", redeemedPoints != null ? redeemedPoints : 0,
                    "reason", reason != null ? reason : "Hoan ve"
            );

            restClient.post()
                    .uri("/internal/v1/loyalty/refund")
                    .header("X-Internal-Service-Secret", internalSecret)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully requested loyalty points refund/restore for userId {} booking {}", userId, bookingCode);
        } catch (Exception ex) {
            log.warn("Failed to refund/restore loyalty points for userId {} booking {}: {}", userId, bookingCode, ex.getMessage());
        }
    }
}
