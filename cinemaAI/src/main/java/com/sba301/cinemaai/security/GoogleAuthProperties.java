package com.sba301.cinemaai.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.google")
public class GoogleAuthProperties {

    private String clientId;
    private String tokenInfoBaseUrl;
}
