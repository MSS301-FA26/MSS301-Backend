package com.cinemaai.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.cinemaai.identity.config.CustomerActivationRabbitConfig;
import com.cinemaai.identity.dto.request.auth.GoogleLoginRequest;
import com.cinemaai.identity.dto.request.auth.RegisterRequest;
import com.cinemaai.identity.entity.RefreshToken;
import com.cinemaai.identity.repository.CustomerActivationOutboxRepository;
import com.cinemaai.identity.repository.PendingRegistrationRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/** Run only against the explicitly owned disposable broker, before Payment's broker integration. */
@SpringJUnitConfig(CustomerActivationBrokerIntegrationTest.BrokerConfiguration.class)
class CustomerActivationBrokerIntegrationTest {
    @Autowired AuthService auth;
    @Autowired PendingRegistrationRepository pending;
    @Autowired UserRepository users;
    @Autowired CustomerActivationOutboxRepository outbox;
    @Autowired CustomerActivationOutboxPublisher publisher;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired RefreshTokenService refreshTokens;
    @Autowired GoogleTokenVerifier googleVerifier;
    @Autowired DataSource dataSource;
    @Autowired CachingConnectionFactory connectionFactory;
    @Autowired ObjectMapper mapper;

    @Test
    void realOtpAndFirstGoogleActivationOutboxAreConfirmedAndQueuedForPayment() throws Exception {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        var exchange = new CustomerActivationRabbitConfig().identityExchange();
        admin.declareExchange(exchange);
        admin.declareExchange(new TopicExchange("cinema.dlx", true, false));
        var queue = QueueBuilder.durable("payment.customer-activated.queue")
                .deadLetterExchange("cinema.dlx").deadLetterRoutingKey("dlq.payment.customer-activated").build();
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("customer.activated"));
        assertEquals(0, admin.getQueueProperties(queue.getName()).get(RabbitAdmin.QUEUE_MESSAGE_COUNT),
                "Use a fresh disposable broker; never purge another workflow's queue");

        new JdbcTemplate(dataSource).update("UPDATE outbox_events SET next_attempt_at = ? WHERE status = 'PENDING'",
                java.sql.Timestamp.valueOf(LocalDateTime.now().plusHours(1)));
        when(passwordEncoder.encode(anyString())).thenReturn("broker-test-password-hash");
        when(refreshTokens.create(any())).thenAnswer(call -> new RefreshToken(call.getArgument(0),
                "broker-test-refresh-token", LocalDateTime.now().plusDays(1)));

        String normalEmail = "normal-broker-" + UUID.randomUUID() + "@example.test";
        auth.register(new RegisterRequest(normalEmail, "Test-password@123", "Test Customer", null, null, null, null, null));
        assertNotNull(pending.findByEmail(normalEmail).orElseThrow());
        auth.verifyEmail(normalEmail, pending.findByEmail(normalEmail).orElseThrow().getOtp());
        Long normalId = users.findByEmail(normalEmail).orElseThrow().getId();

        String googleEmail = "google-broker-" + UUID.randomUUID() + "@example.test";
        when(googleVerifier.verify("broker-test-google-credential")).thenReturn(
                new GoogleTokenVerifier.GoogleTokenInfo("external-test-subject", "test-audience", googleEmail,
                        true, "Google Customer", null, null, null));
        Long googleId = auth.loginWithGoogle(new GoogleLoginRequest("broker-test-google-credential")).user().id();

        publisher.publishPending();

        var fixtures = mapper.createObjectNode();
        for (var pair : java.util.Map.of("normal", normalId, "google", googleId).entrySet()) {
            var event = outbox.findAll().stream().filter(row -> row.getAggregateId().equals(pair.getValue())).findFirst().orElseThrow();
            assertEquals("PUBLISHED", event.getStatus(), "Real mandatory send must be ACKed and routed");
            assertNotNull(event.getPublishedAt());
            fixtures.set(pair.getKey(), mapper.readTree(event.getPayload()));
        }
        assertEquals(2, admin.getQueueProperties(queue.getName()).get(RabbitAdmin.QUEUE_MESSAGE_COUNT));
        Files.writeString(Path.of("target", "customer-activation-events.json"), mapper.writeValueAsString(fixtures));
        // Deliberately leave these two real Identity messages queued for Payment's integration test.
    }

    @Configuration(proxyBeanMethods = false)
    @Import(CustomerActivationPostgresIntegrationTest.PersistenceConfiguration.class)
    static class BrokerConfiguration {
        @Bean(destroyMethod = "destroy")
        CachingConnectionFactory brokerConnectionFactory() {
            int port = Integer.parseInt(java.util.Objects.requireNonNull(System.getProperty("wallet.test.rabbit-port"),
                    "wallet.test.rabbit-port must name the disposable broker"));
            if (port <= 0 || port > 65535 || port == 5672) throw new IllegalArgumentException("Use only a non-production broker port");
            var factory = new CachingConnectionFactory("127.0.0.1", port);
            factory.setUsername("wallet_provision_test");
            factory.setPassword("wallet-test-only");
            factory.setPublisherConfirmType(ConfirmType.CORRELATED);
            factory.setPublisherReturns(true);
            return factory;
        }

        @Bean
        @Primary
        RabbitTemplate brokerRabbitTemplate(CachingConnectionFactory factory) {
            return new CustomerActivationRabbitConfig().rabbitTemplate(factory);
        }
    }
}
