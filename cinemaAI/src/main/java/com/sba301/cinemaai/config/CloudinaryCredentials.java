package com.sba301.cinemaai.config;

import org.springframework.util.StringUtils;

public record CloudinaryCredentials(String cloudName, String apiKey, String apiSecret) {

    public boolean isConfigured() {
        return StringUtils.hasText(cloudName)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(apiSecret);
    }
}
