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
import org.springframework.web.client.RestClientResponseException;

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
            LocalDateTime holdExpiresAt,
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
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .findAndRegisterModules()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.restClient = RestClient.builder()
                .baseUrl(bookingServiceUrl)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof org.springframework.http.converter.json.MappingJackson2HttpMessageConverter);
                    converters.add(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper));
                })
                .build();
    }

    public BookingInfo getBooking(Long bookingId) {
        try {
            ApiResponse<BookingInfo> response = restClient.get()
                    .uri("/internal/v1/bookings/{id}", bookingId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<BookingInfo>>() {});

            if (response == null || response.data() == null) throw new IllegalStateException("Invalid booking-service response");
            return response.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new NotFoundException("Booking not found: " + bookingId);
            throw new IllegalStateException("Booking service lookup failed with HTTP " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Booking service unavailable or returned an invalid response", ex);
        }
    }

    public FoodOrderInfo getFoodOrder(Long foodOrderId) {
        try {
            ApiResponse<FoodOrderInfo> response = restClient.get()
                    .uri("/internal/v1/food-orders/{id}", foodOrderId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<FoodOrderInfo>>() {});

            if (response == null || response.data() == null) throw new IllegalStateException("Invalid booking-service response");
            return response.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new NotFoundException("Food order not found: " + foodOrderId);
            throw new IllegalStateException("Booking service lookup failed with HTTP " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Booking service unavailable or returned an invalid response", ex);
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
            throw new IllegalStateException("Booking mark-paid outcome is unknown", ex);
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
            throw new IllegalStateException("Food-order mark-paid outcome is unknown", ex);
        }
    }
}
