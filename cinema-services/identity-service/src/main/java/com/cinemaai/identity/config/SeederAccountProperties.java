package com.cinemaai.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.seeders.account")
public class SeederAccountProperties {

    private Account admin = new Account();
    private Account manager = new Account();
    private Account staff = new Account();

    @Getter
    @Setter
    public static class Account {
        private String email;
        private String password;
        private String fullName;
        private String phone;
    }
}
