package com.cinemaai.identity.service;

import com.cinemaai.identity.client.CatalogClient;
import com.cinemaai.identity.config.JwtProperties;
import com.cinemaai.identity.dto.request.auth.GoogleLoginRequest;
import com.cinemaai.identity.dto.request.auth.RegisterRequest;
import com.cinemaai.identity.entity.CustomerActivationOutboxEvent;
import com.cinemaai.identity.entity.PendingRegistration;
import com.cinemaai.identity.entity.RefreshToken;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.event.CustomerActivatedEvent;
import com.cinemaai.identity.mapper.UserMapper;
import com.cinemaai.identity.repository.CustomerActivationOutboxRepository;
import com.cinemaai.identity.repository.PendingRegistrationRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.security.JwtService;
import com.cinemaai.identity.service.impl.AuthServiceImpl;
import com.cinemaai.identity.service.impl.EmailVerificationServiceImpl;
import com.cinemaai.identity.service.impl.UserRoleServiceImpl;
import com.cinemaai.identity.service.impl.UserServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManagerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Real PostgreSQL/Flyway/JPA/auth lifecycle; only external auth, mail and token boundaries are doubled. */
@SpringJUnitConfig(CustomerActivationPostgresIntegrationTest.PersistenceConfiguration.class)
class CustomerActivationPostgresIntegrationTest {
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PendingRegistrationRepository pendingRegistrationRepository;
    @Autowired private CustomerActivationOutboxRepository outboxRepository;
    @Autowired private CustomerActivationOutboxService outboxService;
    @Autowired private UserRoleService userRoleService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private GoogleTokenVerifier googleTokenVerifier;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DataSource dataSource;
    @Autowired private CustomerActivationOutboxPublisher publisher;
    @Autowired private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void configureExternalBoundaries() {
        reset(googleTokenVerifier, refreshTokenService, passwordEncoder, objectMapper, rabbitTemplate);
        when(passwordEncoder.encode(anyString())).thenReturn("integration-test-password-hash");
        when(refreshTokenService.create(any(User.class))).thenAnswer(invocation ->
                new RefreshToken(invocation.getArgument(0), "integration-test-refresh-token",
                        LocalDateTime.now().plusDays(1)));
    }

    @Test
    void normalOtpAndFirstGoogleActivationCommitMinimalEventsAndReturningGoogleDoesNotRepeat() throws Exception {
        String normalEmail = uniqueEmail("normal");
        long initialEvents = outboxRepository.count();
        authService.register(registerRequest(normalEmail));
        assertTrue(userRepository.findByEmail(normalEmail).isEmpty());
        assertEquals(initialEvents, outboxRepository.count(), "Pending OTP must not create an activation event");
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(normalEmail).orElseThrow();

        authService.verifyEmail(normalEmail, pending.getOtp());

        User normal = userRepository.findByEmail(normalEmail).orElseThrow();
        assertTrue(normal.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, normal.getStatus());
        assertTrue(pendingRegistrationRepository.findByEmail(normalEmail).isEmpty());
        CustomerActivationOutboxEvent normalEvent = onlyEventFor(normal.getId());

        String googleEmail = uniqueEmail("google");
        when(googleTokenVerifier.verify("integration-test-google-credential")).thenReturn(
                new GoogleTokenVerifier.GoogleTokenInfo("external-google-subject", "test-audience", googleEmail,
                        true, "Google Customer", null, null, null));
        var firstLogin = authService.loginWithGoogle(new GoogleLoginRequest("integration-test-google-credential"));
        Long googleId = firstLogin.user().id();
        User google = userRepository.findByEmail(googleEmail).orElseThrow();
        assertEquals(google.getId(), googleId);
        assertTrue(google.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, google.getStatus());
        CustomerActivationOutboxEvent googleEvent = onlyEventFor(googleId);

        var returningLogin = authService.loginWithGoogle(new GoogleLoginRequest("integration-test-google-credential"));
        assertEquals(googleId, returningLogin.user().id());
        assertEquals(initialEvents + 2, outboxRepository.count());
        assertEquals(googleEvent.getEventId(), onlyEventFor(googleId).getEventId());

        ObjectNode fixture = objectMapper.createObjectNode();
        fixture.set("normal", assertMinimalEvent(normalEvent, normal.getId()));
        fixture.set("google", assertMinimalEvent(googleEvent, googleId));
        Path output = Path.of("target", "customer-activation-events.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, objectMapper.writeValueAsString(fixture));
    }

    @Test
    void accountStateAndOutboxRollBackTogetherWhenActivationTransactionRollsBack() {
        String email = uniqueEmail("rollback");
        long initialEvents = outboxRepository.count();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            authService.register(registerRequest(email));
            PendingRegistration pending = pendingRegistrationRepository.findByEmail(email).orElseThrow();
            authService.verifyEmail(email, pending.getOtp());
            assertTrue(userRepository.findByEmail(email).isPresent());
            throw new IllegalStateException("Deliberate test transaction rollback");
        }));

        assertTrue(userRepository.findByEmail(email).isEmpty());
        assertTrue(pendingRegistrationRepository.findByEmail(email).isEmpty());
        assertEquals(initialEvents, outboxRepository.count());
    }

    @Test
    void serializationFailureRollsBackUserCreationAndLeavesPendingRegistrationRetryable() throws Exception {
        String email = uniqueEmail("serialization");
        authService.register(registerRequest(email));
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email).orElseThrow();
        long initialEvents = outboxRepository.count();
        doThrow(new JsonProcessingException("Deliberate test serialization failure") {})
                .when(objectMapper).writeValueAsString(any(CustomerActivatedEvent.class));

        assertThrows(IllegalStateException.class, () -> authService.verifyEmail(email, pending.getOtp()));

        assertTrue(userRepository.findByEmail(email).isEmpty());
        assertTrue(pendingRegistrationRepository.findByEmail(email).isPresent());
        assertEquals(initialEvents, outboxRepository.count());
    }

    @Test
    void concurrentReplayCreatesOneSemanticActivationEvent() throws Exception {
        Long userId = createActiveUser(RoleName.CUSTOMER);
        int concurrency = 4;
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(concurrency);
        try {
            var deliveries = java.util.stream.IntStream.range(0, concurrency).mapToObj(index ->
                    executor.submit(() -> {
                        ready.countDown();
                        try {
                            if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Concurrent start timed out");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                                outboxService.recordActivation(userRepository.findById(userId).orElseThrow()));
                    })).toList();
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> delivery : deliveries) delivery.get(20, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
        assertMinimalEvent(onlyEventFor(userId), userId);
    }

    @Test
    void activeStaffIsNotEligibleForCustomerWalletProvisioning() {
        Long userId = createActiveUser(RoleName.STAFF);

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                outboxService.recordActivation(userRepository.findById(userId).orElseThrow()));

        assertTrue(outboxRepository.findAll().stream().noneMatch(event -> event.getAggregateId().equals(userId)));
    }

    @Test
    void confirmedPublisherCommitsPublishedStateThroughRealJpaDirtyTracking() {
        Long userId = isolatePendingActivation();
        doAnswer(call -> {
            call.<CorrelationData>getArgument(4).getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        publisher.publishPending();

        var event = eventFor(userId);
        assertEquals("PUBLISHED", event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertEquals(0, event.getRetryCount());
    }

    @Test
    void offlinePublisherCommitsBackoffAndDoesNotImmediatelyRepublish() {
        Long userId = isolatePendingActivation();
        doThrow(new org.springframework.amqp.AmqpConnectException(new java.net.ConnectException("test offline")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        publisher.publishPending();
        publisher.publishPending();

        var event = eventFor(userId);
        assertEquals("PENDING", event.getStatus());
        assertNull(event.getPublishedAt());
        assertEquals(1, event.getRetryCount());
        assertTrue(event.getNextAttemptAt().isAfter(event.getCreatedAt()));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void anotherPublisherSkipsRowsLockedByAnActivePublisherTransaction() throws Exception {
        Long userId = isolatePendingActivation();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var owner = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertEquals(userId, outboxRepository.lockPending(LocalDateTime.now(), 10).getFirst().getAggregateId());
                locked.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Lock release timed out");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }));
            assertTrue(locked.await(10, TimeUnit.SECONDS));
            var skipped = new TransactionTemplate(transactionManager).execute(status ->
                    outboxRepository.lockPending(LocalDateTime.now(), 10));
            assertTrue(skipped.isEmpty(), "Concurrent publisher must not claim a locked row");
            release.countDown();
            owner.get(10, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private Long isolatePendingActivation() {
        // Only owned disposable test data: make unrelated rows ineligible for this publisher assertion.
        new JdbcTemplate(dataSource).update("UPDATE outbox_events SET next_attempt_at = ? WHERE status = 'PENDING'",
                java.sql.Timestamp.valueOf(LocalDateTime.now().plusHours(1)));
        Long userId = createActiveUser(RoleName.CUSTOMER);
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                outboxService.recordActivation(userRepository.findById(userId).orElseThrow()));
        return userId;
    }

    private CustomerActivationOutboxEvent eventFor(Long userId) {
        return outboxRepository.findAll().stream().filter(event -> event.getAggregateId().equals(userId)).findFirst().orElseThrow();
    }

    private Long createActiveUser(RoleName role) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            User user = userRepository.save(new User(uniqueEmail(role.name().toLowerCase()),
                    "integration-test-password-hash", "Test Customer", null));
            user.setEmailVerified(true);
            user.setStatus(UserStatus.ACTIVE);
            userRoleService.assignRole(user, role);
            return user.getId();
        });
    }

    private CustomerActivationOutboxEvent onlyEventFor(Long userId) {
        List<CustomerActivationOutboxEvent> events = outboxRepository.findAll().stream()
                .filter(event -> event.getAggregateId().equals(userId)).toList();
        assertEquals(1, events.size());
        assertEquals("PENDING", events.getFirst().getStatus());
        return events.getFirst();
    }

    private com.fasterxml.jackson.databind.JsonNode assertMinimalEvent(CustomerActivationOutboxEvent event, Long userId)
            throws JsonProcessingException {
        var payload = objectMapper.readTree(event.getPayload());
        assertEquals(userId.longValue(), payload.get("userId").longValue());
        assertEquals(event.getEventId(), payload.get("eventId").textValue());
        assertEquals(4, payload.size(), "Only eventId, userId, occurredAt and correlationId may cross this boundary");
        assertTrue(payload.hasNonNull("occurredAt"));
        assertTrue(payload.hasNonNull("correlationId"));
        assertFalse(payload.has("email"));
        assertFalse(payload.has("password"));
        assertFalse(payload.has("credential"));
        assertFalse(payload.has("accessToken"));
        assertFalse(payload.has("refreshToken"));
        return payload;
    }

    private static RegisterRequest registerRequest(String email) {
        return new RegisterRequest(email, "Test-password@123", "Test Customer", null, null, null, null, null);
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.test";
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaAuditing
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    @Import({AuthServiceImpl.class, EmailVerificationServiceImpl.class, UserRoleServiceImpl.class,
            UserServiceImpl.class, CustomerActivationOutboxService.class, CustomerActivationOutboxPublisher.class, UserMapper.class})
    static class PersistenceConfiguration {
        @Bean
        DataSource dataSource() {
            String jdbcUrl = requiredProperty("wallet.test.jdbc-url");
            String username = requiredProperty("wallet.test.username");
            String password = requiredProperty("wallet.test.password");
            if (!jdbcUrl.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/wallet_identity_test")
                    || java.net.URI.create(jdbcUrl.substring("jdbc:".length())).getPort() == 5432
                    || !username.equals("wallet_provision_test")) {
                throw new IllegalArgumentException("Integration tests require the explicitly named disposable Identity database");
            }
            return new DriverManagerDataSource(jdbcUrl, username, password);
        }

        @Bean(initMethod = "migrate")
        Flyway flyway(DataSource dataSource) {
            return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, Flyway flyway) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.cinemaai.identity.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
            return factory;
        }

        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean ObjectMapper objectMapper() {
            return spy(new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        }
        @Bean PasswordEncoder passwordEncoder() { return mock(PasswordEncoder.class); }
        @Bean AuthenticationManager authenticationManager() { return mock(AuthenticationManager.class); }
        @Bean JwtService jwtService() { return mock(JwtService.class); }
        @Bean JwtProperties jwtProperties() { return mock(JwtProperties.class); }
        @Bean RefreshTokenService refreshTokenService() { return mock(RefreshTokenService.class); }
        @Bean UserCinemaAssignmentService userCinemaAssignmentService() { return mock(UserCinemaAssignmentService.class); }
        @Bean GoogleTokenVerifier googleTokenVerifier() { return mock(GoogleTokenVerifier.class); }
        @Bean CatalogClient catalogClient() { return mock(CatalogClient.class); }
        @Bean MailService mailService() { return mock(MailService.class); }
        @Bean AuditLogService auditLogService() { return mock(AuditLogService.class); }
        @Bean RabbitTemplate rabbitTemplate() { return mock(RabbitTemplate.class); }

        private static String requiredProperty(String key) {
            String value = System.getProperty(key);
            if (value == null) throw new IllegalArgumentException("Required integration property is missing: " + key);
            return value;
        }
    }
}
