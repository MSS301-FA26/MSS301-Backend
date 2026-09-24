package com.cinemaai.payment.client;

import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.exception.NotFoundException;
import java.math.BigDecimal;
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

    public record BookingInfo(
            Long id,
            String bookingCode,
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
}
