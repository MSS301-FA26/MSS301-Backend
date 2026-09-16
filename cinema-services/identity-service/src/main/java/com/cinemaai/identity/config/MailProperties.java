package com.cinemaai.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private boolean enabled = true;
    private String from = "iwakeupwhen@gmail.com";
}
