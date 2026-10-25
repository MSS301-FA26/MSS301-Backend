package com.cinemaai.booking.client;

import com.cinemaai.booking.client.dto.CatalogProductQuoteDto;
import com.cinemaai.booking.client.dto.CatalogQuoteDto;
import com.cinemaai.booking.client.dto.ReportingShowtimeDto;
import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.ForbiddenException;
import com.cinemaai.booking.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Slf4j
@Component
public class CatalogClient {

    private final RestClient restClient;
    private final String catalogServiceUrl;
    private final String internalSecret;

    public CatalogClient(
            @Value("${catalog.service.url}") String catalogServiceUrl,
            @Value("${app.internal.secret}") String internalSecret,
            @Value("${catalog.service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${catalog.service.read-timeout-ms:3000}") int readTimeoutMs
    ) {
        this.catalogServiceUrl = catalogServiceUrl;
        this.internalSecret = internalSecret;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder().baseUrl(catalogServiceUrl)
                .requestFactory(requestFactory).build();
    }

    public record ShowtimeScope(Long showtimeId, Long cinemaId) {}

    public List<ReportingShowtimeDto> getReportingShowtimes(LocalDate from, LocalDate to, Long cinemaId) {
        ApiResponse<List<ReportingShowtimeDto>> response = restClient.get()
                .uri(uri -> uri.path("/internal/v1/catalog/showtimes/reporting")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .queryParamIfPresent("cinemaId", Optional.ofNullable(cinemaId))
                        .build())
                .header("X-Internal-Service-Secret", internalSecret)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<List<ReportingShowtimeDto>>>() {});
        if (response == null || !response.success() || response.data() == null) {
            throw new IllegalStateException("Catalog service returned invalid reporting showtimes");
        }
        return response.data();
    }

    public ShowtimeScope getShowtimeScope(Long showtimeId) {
        try {
            ApiResponse<ShowtimeScope> response = restClient.get()
                    .uri("/internal/v1/catalog/showtimes/{showtimeId}/scope", showtimeId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<ShowtimeScope>>() {});
            if (response == null || response.data() == null
                    || !showtimeId.equals(response.data().showtimeId())
                    || response.data().cinemaId() == null || response.data().cinemaId() <= 0) {
                throw new ForbiddenException("Unable to verify showtime cinema");
            }
            return response.data();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Showtime not found");
        } catch (NotFoundException | ForbiddenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to resolve cinema for showtime {}: {}", showtimeId, ex.getMessage());
            throw new ForbiddenException("Unable to verify showtime cinema");
        }
    }

    public CatalogQuoteDto.Response getQuote(CatalogQuoteDto.Request request) {
        try {
            ApiResponse<CatalogQuoteDto.Response> response = restClient.post()
                    .uri("/internal/v1/catalog/checkout-quote")
                    .header("X-Internal-Service-Secret", internalSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<CatalogQuoteDto.Response>>() {});

            if (response == null || response.data() == null) {
                throw new BadRequestException("Catalog service returned empty quote");
            }
            return response.data();
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            log.error("Failed to fetch quote from Catalog Service: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            String extractedMessage = null;
            try {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(ex.getResponseBodyAsString());
                if (root.hasNonNull("message")) {
                    extractedMessage = root.get("message").asText();
                }
            } catch (Exception ignored) {}
            if (extractedMessage != null && !extractedMessage.isBlank()) {
                throw new BadRequestException(extractedMessage);
            }
            throw new BadRequestException("Suất chiếu không khả dụng hoặc chưa mở bán vé.");
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to fetch quote from Catalog Service: {}", ex.getMessage());
            throw new BadRequestException("Không thể kết nối đến hệ thống Catalog: " + ex.getMessage());
        }
    }

    public CatalogProductQuoteDto.Response getFoodQuote(CatalogProductQuoteDto.Request request) {
        try {
            ApiResponse<CatalogProductQuoteDto.Response> response = restClient.post()
                    .uri("/internal/v1/foods/quote")
                    .header("X-Internal-Service-Secret", internalSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<CatalogProductQuoteDto.Response>>() {});

            if (response != null && response.data() != null) {
                return response.data();
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch food quote from /internal/v1/foods/quote: {}", ex.getMessage());
        }
        return null;
    }
}
