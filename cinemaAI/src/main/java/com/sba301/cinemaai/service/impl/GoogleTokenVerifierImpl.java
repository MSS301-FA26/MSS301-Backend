package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.UnauthorizedException;
import com.sba301.cinemaai.security.GoogleAuthProperties;
import com.sba301.cinemaai.service.GoogleTokenVerifier;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private static final String DEFAULT_TOKEN_INFO_BASE_URL = "https://oauth2.googleapis.com";

    private final GoogleAuthProperties googleAuthProperties;
    private final RestClient restClient;

    public GoogleTokenVerifierImpl(GoogleAuthProperties googleAuthProperties) {
        this.googleAuthProperties = googleAuthProperties;
        this.restClient = RestClient.builder()
                .baseUrl(resolveTokenInfoBaseUrl(googleAuthProperties.getTokenInfoBaseUrl()))
                .build();
    }

    public GoogleTokenInfo verify(String credential) {
        String configuredClientId = googleAuthProperties.getClientId() == null ? null : googleAuthProperties.getClientId().trim();
        if (configuredClientId == null || configuredClientId.isBlank()) {
            throw new BadRequestException("Google client id is not configured");
        }

        GoogleTokenInfo tokenInfo;
        try {
            tokenInfo = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tokeninfo")
                            .queryParam("id_token", credential)
                            .build())
                    .retrieve()
                    .body(GoogleTokenInfo.class);
        } catch (RestClientResponseException exception) {
            throw new UnauthorizedException("Invalid Google credential: Google rejected the token");
        }

        if (tokenInfo == null
                || tokenInfo.email() == null
                || tokenInfo.email().isBlank()) {
            throw new UnauthorizedException("Invalid Google credential");
        }
        if (!configuredClientId.equals(tokenInfo.audience())) {
            throw new UnauthorizedException(
                    "Invalid Google credential: audience does not match configured client id"
                            + " (aud=" + tokenInfo.audience()
                            + ", configured=" + configuredClientId + ")"
            );
        }
        if (!Boolean.TRUE.equals(tokenInfo.emailVerified())) {
            throw new UnauthorizedException("Google email is not verified");
        }

        return tokenInfo;
    }

    private String resolveTokenInfoBaseUrl(String configuredBaseUrl) {
        String baseUrl = configuredBaseUrl == null || configuredBaseUrl.isBlank()
                ? DEFAULT_TOKEN_INFO_BASE_URL
                : configuredBaseUrl.trim();
        URI uri = URI.create(baseUrl);
        return uri.getScheme() == null ? "https://" + baseUrl : baseUrl;
    }
}
