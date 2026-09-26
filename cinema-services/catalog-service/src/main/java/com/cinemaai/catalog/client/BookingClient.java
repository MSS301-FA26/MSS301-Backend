package com.cinemaai.catalog.client;

import com.cinemaai.catalog.dto.response.ApiResponse;
import java.time.LocalDateTime;
import java.util.Collections;
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

    public BookingClient(
            @Value("${booking.service.url}") String bookingServiceUrl,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.internalSecret = internalSecret;
        this.restClient = RestClient.builder().baseUrl(bookingServiceUrl).build();
    }

    public List<OccupiedSeat> getOccupiedSeats(Long showtimeId) {
        try {
            ApiResponse<List<OccupiedSeat>> response = restClient.get()
                    .uri("/internal/v1/bookings/showtimes/{showtimeId}/occupied-seats", showtimeId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<OccupiedSeat>>>() {});

            if (response != null && response.data() != null) {
                return response.data();
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch occupied seats for showtimeId={} from booking-service: {}",
                    showtimeId, ex.getMessage());
        }
        return Collections.emptyList();
    }
}
