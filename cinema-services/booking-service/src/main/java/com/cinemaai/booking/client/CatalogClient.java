package com.cinemaai.booking.client;

import com.cinemaai.booking.client.dto.CatalogFoodQuoteDto;
import com.cinemaai.booking.client.dto.CatalogQuoteDto;
import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class CatalogClient {

    private final RestClient restClient;
    private final String catalogServiceUrl;
    private final String internalSecret;

    public CatalogClient(
            @Value("${catalog.service.url}") String catalogServiceUrl,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.catalogServiceUrl = catalogServiceUrl;
        this.internalSecret = internalSecret;
        this.restClient = RestClient.builder().baseUrl(catalogServiceUrl).build();
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

    public CatalogFoodQuoteDto.Response getFoodQuote(CatalogFoodQuoteDto.Request request) {
        try {
            ApiResponse<CatalogFoodQuoteDto.Response> response = restClient.post()
                    .uri("/internal/v1/foods/quote")
                    .header("X-Internal-Service-Secret", internalSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<CatalogFoodQuoteDto.Response>>() {});

            if (response != null && response.data() != null) {
                return response.data();
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch food quote from /internal/v1/foods/quote: {}", ex.getMessage());
        }
        return null;
    }
}