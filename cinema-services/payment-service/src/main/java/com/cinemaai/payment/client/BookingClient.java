package com.cinemaai.payment.client;

import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.exception.NotFoundException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class BookingClient {

    private final RestClient restClient;
    private final String internalSecret;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookingInfo(
            Long id,
            String bookingCode,
            Long userId,
            BigDecimal totalAmount,
            String status,
            Integer loyaltyPointsRedeemed,
            Long cinemaId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FoodOrderInfo(
            Long id,
            String foodOrderCode,
            Long userId,
            BigDecimal totalAmount,
            String status
    ) {}

    public BookingClient(
            @Value("${booking.service.url}") String bookingServiceUrl,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.internalSecret = internalSecret;
        this.restClient = RestClient.builder().baseUrl(bookingServiceUrl).build();
    }

    public BookingInfo getBooking(Long bookingId) {
        try {
            ApiResponse<BookingInfo> response = restClient.get()
                    .uri("/internal/v1/bookings/{id}", bookingId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<BookingInfo>>() {});

            if (response == null || response.data() == null) {
                throw new NotFoundException("Booking info not found from booking service");
            }
            return response.data();
        } catch (Exception ex) {
            log.error("Failed to query booking {} from booking-service: {}", bookingId, ex.getMessage());
            throw new NotFoundException("Không tìm thấy thông tin đặt vé: " + ex.getMessage());
        }
    }

    public FoodOrderInfo getFoodOrder(Long foodOrderId) {
        try {
            ApiResponse<FoodOrderInfo> response = restClient.get()
                    .uri("/internal/v1/food-orders/{id}", foodOrderId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<FoodOrderInfo>>() {});

            if (response == null || response.data() == null) {
                throw new NotFoundException("Food order info not found from booking service");
            }
            return response.data();
        } catch (Exception ex) {
            log.error("Failed to query food order {} from booking-service: {}", foodOrderId, ex.getMessage());
            throw new NotFoundException("Không tìm thấy thông tin đơn bắp nước: " + ex.getMessage());
        }
    }

    public void markBookingPaid(Long bookingId, String transactionId, LocalDateTime paidAt) {
        try {
            restClient.post()
                    .uri("/internal/v1/bookings/{bookingId}/mark-paid?transactionId={txn}",
                            bookingId, transactionId != null ? transactionId : "")
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Directly marked booking {} as PAID via internal REST call", bookingId);
        } catch (Exception ex) {
            log.warn("Direct REST markBookingPaid failed for booking {}: {}", bookingId, ex.getMessage());
        }
    }

    public void markFoodOrderPaid(Long foodOrderId, String transactionId, LocalDateTime paidAt) {
        try {
            restClient.post()
                    .uri("/internal/v1/food-orders/{foodOrderId}/mark-paid?transactionId={txn}",
                            foodOrderId, transactionId != null ? transactionId : "")
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Directly marked food order {} as PAID via internal REST call", foodOrderId);
        } catch (Exception ex) {
            log.warn("Direct REST markFoodOrderPaid failed for food order {}: {}", foodOrderId, ex.getMessage());
        }
    }
}
