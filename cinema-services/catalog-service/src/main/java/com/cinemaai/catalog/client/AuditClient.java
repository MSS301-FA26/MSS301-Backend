package com.cinemaai.catalog.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class AuditClient {

    private final RestClient restClient;
    private final String internalSecret;

    public record InternalAuditRequest(
            String action,
            String targetType,
            Long targetId,
            String detail,
            Long actorId
    ) {}

    public AuditClient(
            @Value("${identity.service.url}") String identityServiceUrl,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.internalSecret = internalSecret;
        this.restClient = RestClient.builder().baseUrl(identityServiceUrl).build();
    }

    public void record(String action, String targetType, Long targetId, String detail, Long actorId) {
        try {
            restClient.post()
                    .uri("/internal/v1/audit-logs")
                    .header("X-Internal-Service-Secret", internalSecret)
                    .body(new InternalAuditRequest(action, targetType, targetId, detail, actorId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to record audit log in identity-service: {}", ex.getMessage());
        }
    }
}
