package com.cinemaai.catalog.client;

import com.cinemaai.catalog.dto.response.ApiResponse;
import java.time.LocalDateTime;
import com.cinemaai.catalog.exception.SeatAvailabilityUnavailableException;
import java.util.List;
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

    public record OccupiedSeat(
            Long seatId,
            String runtimeStatus,
            LocalDateTime holdExpiresAt
    ) {}

    public record BookingEligibilityDto(
            boolean hasWatched,
            Long bookingId,
            String bookingCode,
            LocalDateTime showtimeStart,
            String cinemaName,
            String status
    ) {}

    public BookingClient(
            @Value("${booking.service.url}") String bookingServiceUrl,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.internalSecret = internalSecret;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = RestClient.builder().baseUrl(bookingServiceUrl).requestFactory(factory).build();
    }

    public List<OccupiedSeat> getOccupiedSeats(Long showtimeId) {
        try {
            ApiResponse<List<OccupiedSeat>> response = restClient.get()
                    .uri("/internal/v1/bookings/showtimes/{showtimeId}/occupied-seats", showtimeId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<OccupiedSeat>>>() {});

            if (response != null && response.success() && response.data() != null) {
                var seen = new java.util.HashSet<Long>();
                for (OccupiedSeat seat : response.data()) {
                    if (seat == null || seat.seatId() == null || seat.seatId() <= 0
                            || !seen.add(seat.seatId()) || seat.runtimeStatus() == null
                            || !java.util.Set.of("HOLDING", "BOOKED", "UNAVAILABLE").contains(seat.runtimeStatus())) {
                        throw new SeatAvailabilityUnavailableException();
                    }
                }
                return response.data();
            }
        } catch (Exception ex) {
            log.warn("Occupancy verification failed for showtimeId={} ({})", showtimeId, ex.getClass().getSimpleName());
            throw new SeatAvailabilityUnavailableException();
        }
        throw new SeatAvailabilityUnavailableException();
    }

    public java.util.Optional<BookingEligibilityDto> checkBookingEligibility(Long userId, Long movieId) {
        try {
            ApiResponse<BookingEligibilityDto> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/v1/bookings/verify-eligibility")
                            .queryParam("userId", userId)
                            .queryParam("movieId", movieId)
                            .build())
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<BookingEligibilityDto>>() {});

            if (response != null && response.data() != null) {
                return java.util.Optional.of(response.data());
            }
        } catch (Exception ex) {
            log.warn("Failed to check booking eligibility for userId={} movieId={} from booking-service: {}",
                    userId, movieId, ex.getMessage());
        }
        return java.util.Optional.empty();
    }

    public record ShowtimeCancelRefundResult(
            Long showtimeId,
            int totalBookingsProcessed,
            int refundedTicketsCount,
            int cancelledTicketsCount,
            java.math.BigDecimal totalRefundedAmount,
            String message
    ) {}

    public ShowtimeCancelRefundResult cancelAndRefundShowtime(Long showtimeId, String reason, String actorRole, Long actorUserId) {
        try {
            java.util.Map<String, Object> body = java.util.Map.of(
                    "reason", reason != null ? reason : "Su co ky thuat phong chieu",
                    "actorRole", actorRole != null ? actorRole : "ADMIN",
                    "actorUserId", actorUserId != null ? actorUserId : 1L
            );

            ApiResponse<ShowtimeCancelRefundResult> response = restClient.post()
                    .uri("/internal/v1/bookings/showtimes/{showtimeId}/cancel-and-refund", showtimeId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<ShowtimeCancelRefundResult>>() {});

            if (response != null && response.data() != null) {
                log.info("Coordinated cancellation with booking-service for showtimeId {}: {}", showtimeId, response.data());
                return response.data();
            }
        } catch (Exception ex) {
            log.error("Failed to coordinate cancellation and refund with booking-service for showtimeId {}: {}", showtimeId, ex.getMessage());
        }
        return null;
    }

}
