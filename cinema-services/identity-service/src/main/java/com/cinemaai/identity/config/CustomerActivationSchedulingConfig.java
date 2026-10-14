package com.cinemaai.identity.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "customer-activation.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class CustomerActivationSchedulingConfig {
}
