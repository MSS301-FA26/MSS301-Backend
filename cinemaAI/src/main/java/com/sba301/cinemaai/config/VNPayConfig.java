package com.sba301.cinemaai.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@RequiredArgsConstructor
public class VNPayConfig {

    private final VnpayProperties properties;

    public String getTmnCode() {
        return properties.getTmnCode();
    }

    public String getHashSecret() {
        return properties.getHashSecret();
    }

    public String getPaymentUrl() {
        return properties.getApiUrl();
    }

    public String getReturnUrl() {
        return properties.getReturnUrl();
    }

    public String getIpnUrl() {
        return properties.getIpnUrl();
    }

    public String getDefaultClientIp() {
        return properties.getDefaultClientIp();
    }
}
