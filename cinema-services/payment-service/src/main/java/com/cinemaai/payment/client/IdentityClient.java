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
        this.restClient = RestClient.builder().baseUrl(identityServiceUrl).build();
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
