package com.cinemaai.payment.client;

import com.cinemaai.payment.dto.response.ApiResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class IdentityClient {

    private final RestClient restClient;
    private final String internalSecret;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserAccessScope(Long userId, String status, java.util.List<String> roles, Long cinemaId) {}

    public UserAccessScope getUserAccessScope(Long userId) {
        if (userId == null || userId <= 0) return null;
        try {
            ApiResponse<UserAccessScope> response = restClient.get()
                    .uri("/internal/v1/users/{userId}/scope", userId)
                    .header("X-Internal-Service-Secret", internalSecret).retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<UserAccessScope>>() {});
            return response != null && response.success() ? response.data() : null;
        } catch (Exception ex) {
            log.warn("Identity scope verification unavailable");
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserProfileDto(
            Long id,
            String email,
            String fullName,
            String phone
    ) {}

    public IdentityClient(
            @Value("${identity.service.url:http://localhost:8081}") String identityServiceUrl,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.internalSecret = internalSecret;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = RestClient.builder().baseUrl(identityServiceUrl).requestFactory(factory).build();
    }

    public UserProfileDto getUserProfile(Long userId) {
        if (userId == null) return null;
        try {
            ApiResponse<UserProfileDto> response = restClient.get()
                    .uri("/internal/v1/users/{userId}", userId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<UserProfileDto>>() {});
            return response != null ? response.data() : null;
        } catch (Exception ex) {
            log.warn("Failed to get user profile from identity-service for userId {}: {}", userId, ex.getMessage());
            return null;
        }
    }
}
