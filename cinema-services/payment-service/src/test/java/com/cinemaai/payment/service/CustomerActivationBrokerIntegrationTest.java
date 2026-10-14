package com.cinemaai.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinemaai.payment.config.RabbitMqConfig;
import com.cinemaai.payment.controller.WalletController;
import com.cinemaai.payment.exception.GlobalExceptionHandler;
import com.cinemaai.payment.listener.CustomerActivatedEventListener;
import com.cinemaai.payment.security.AuthenticatedUser;
import com.cinemaai.payment.service.impl.WalletServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerActivationBrokerIntegrationTest {

    private static final RabbitMqConfig RABBIT_CONFIG = new RabbitMqConfig();
    private static AnnotationConfigApplicationContext databaseContext;
    private static CachingConnectionFactory connectionFactory;
    private static RabbitAdmin admin;
    private static RabbitTemplate rabbit;
    private static JdbcTemplate jdbc;
    private static ObjectMapper mapper;
    private static String normalEvent;
    private static long normalUserId;
    private static long googleUserId;
    private static volatile CountDownLatch successfulAnnotationDeliveries;
    private static SimpleMessageListenerContainer annotatedContainer;

    @BeforeAll
    static void setUpIsolatedBrokerAndDatabase() throws Exception {
        String portProperty = System.getProperty("wallet.test.rabbit-port");
        if (portProperty == null) throw new IllegalArgumentException("Required integration property: wallet.test.rabbit-port");
        int port = Integer.parseInt(portProperty);
        if (port <= 0 || port > 65535 || port == 5672) {
            throw new IllegalArgumentException("Use only an isolated loopback RabbitMQ broker on a non-production port");
        }
        var configuration = new WalletProvisioningPostgresIntegrationTest.DatabaseConfiguration();
        var dataSource = configuration.dataSource();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .schemas("payment_wallet_provision_it").defaultSchema("payment_wallet_provision_it").load().migrate();
        databaseContext = new AnnotationConfigApplicationContext();
        databaseContext.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolatedRabbit", Map.of(
                "spring.rabbitmq.host", "127.0.0.1",
                "spring.rabbitmq.port", Integer.toString(port),
                "spring.rabbitmq.username", "wallet_provision_test",
                "spring.rabbitmq.password", "wallet-test-only",
                "spring.rabbitmq.listener.simple.auto-startup", "false")));
        databaseContext.register(WalletProvisioningPostgresIntegrationTest.DatabaseConfiguration.class,
                RabbitMqConfig.class, RabbitAutoConfiguration.class, AnnotationObservationConfiguration.class);
        databaseContext.refresh();
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("TRUNCATE cine_wallets CASCADE");
        mapper = databaseContext.getBean(ObjectMapper.class);
        var fixtures = mapper.readTree(Files.readString(Path.of(System.getProperty("wallet.test.event-fixtures",
                "../identity-service/target/customer-activation-events.json"))));
        assertNotNull(fixtures.get("normal"));
        assertNotNull(fixtures.get("google"));
        normalEvent = fixtures.get("normal").toString();
        normalUserId = fixtures.get("normal").get("userId").longValue();
        googleUserId = fixtures.get("google").get("userId").longValue();
        assertTrue(normalUserId > 0 && googleUserId > 0 && normalUserId != googleUserId);

        connectionFactory = databaseContext.getBean(CachingConnectionFactory.class);
        admin = new RabbitAdmin(connectionFactory);
        admin.declareExchange(RABBIT_CONFIG.identityExchange());
        admin.declareExchange(RABBIT_CONFIG.deadLetterExchange());
        var queue = RABBIT_CONFIG.customerActivatedQueue();
        var dlq = RABBIT_CONFIG.customerActivatedDeadLetterQueue();
        admin.declareQueue(queue);
        admin.declareQueue(dlq);
        admin.declareBinding(RABBIT_CONFIG.customerActivatedBinding(queue, RABBIT_CONFIG.identityExchange()));
        admin.declareBinding(RABBIT_CONFIG.customerActivatedDeadLetterBinding(dlq, RABBIT_CONFIG.deadLetterExchange()));
        rabbit = RABBIT_CONFIG.rabbitTemplate(connectionFactory, RABBIT_CONFIG.jsonMessageConverter());
        assertEquals(2, admin.getQueueInfo(RabbitMqConfig.CUSTOMER_ACTIVATED_QUEUE).getMessageCount(),
                "Identity broker integration must first queue its real OTP and Google activation events");
        var annotation = CustomerActivatedEventListener.class.getMethod("onCustomerActivated", String.class)
                .getAnnotation(RabbitListener.class);
        assertNotNull(annotation, "The production listener must remain annotation-discoverable");
        assertEquals("customerActivationListenerContainerFactory", annotation.containerFactory());
        var containers = databaseContext.getBean(RabbitListenerEndpointRegistry.class).getListenerContainers();
        assertEquals(1, containers.size(), "The production @RabbitListener must register exactly one consumer");
        annotatedContainer = (SimpleMessageListenerContainer) containers.iterator().next();
        assertEquals(List.of(RabbitMqConfig.CUSTOMER_ACTIVATED_QUEUE), List.of(annotatedContainer.getQueueNames()));
        assertTrue(!annotatedContainer.isRunning(), "Do not consume the Identity messages before the test starts");
        // Never purge the two Identity messages: consuming them is the cross-service proof.
    }

    @AfterAll
    static void closeResources() {
        if (connectionFactory != null) connectionFactory.destroy();
        if (databaseContext != null) databaseContext.close();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Order(1)
    void actualIdentityOutboxMessagesProvisionBothWalletsAndReplayPreservesBalance() throws Exception {
        CountDownLatch consumed = new CountDownLatch(2);
        successfulAnnotationDeliveries = consumed;
        annotatedContainer.start();
        try {
            assertTrue(consumed.await(20, TimeUnit.SECONDS), "Both actual Identity broker messages must be consumed");
        } finally {
            annotatedContainer.stop();
        }
        assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM cine_wallets", Long.class));
        assertWalletHttp200(normalUserId, BigDecimal.ZERO);
        assertWalletHttp200(googleUserId, BigDecimal.ZERO);
        assertEquals(0, admin.getQueueInfo(RabbitMqConfig.CUSTOMER_ACTIVATED_QUEUE).getMessageCount());

        jdbc.update("UPDATE cine_wallets SET balance = ? WHERE user_id = ?", new BigDecimal("85000.00"), normalUserId);
        var before = databaseContext.getBean(WalletServiceImpl.class).getWallet(normalUserId);
        rabbit.convertAndSend(RabbitMqConfig.IDENTITY_EXCHANGE,
                RabbitMqConfig.CUSTOMER_ACTIVATED_ROUTING_KEY, normalEvent);
        CountDownLatch replayed = new CountDownLatch(1);
        successfulAnnotationDeliveries = replayed;
        annotatedContainer.start();
        try {
            assertTrue(replayed.await(10, TimeUnit.SECONDS), "Duplicate delivery must finish normally");
        } finally {
            annotatedContainer.stop();
        }
        var after = databaseContext.getBean(WalletServiceImpl.class).getWallet(normalUserId);
        assertEquals(before.id(), after.id());
        assertEquals(before.createdAt(), after.createdAt());
        assertEquals(new BigDecimal("85000.00"), after.balance());
        assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM cine_wallets", Long.class));
        assertWalletHttp200(normalUserId, new BigDecimal("85000.00"));
    }

    @Test
    @Order(2)
    void actualBrokerRejectsDatabaseFailureToTheConfiguredDlqAfterThreeAttempts() throws Exception {
        assertEquals(0, admin.getQueueInfo(RabbitMqConfig.CUSTOMER_ACTIVATED_DLQ).getMessageCount());
        WalletProvisioningService unavailableDatabase = mock(WalletProvisioningService.class);
        when(unavailableDatabase.ensureWalletExists(normalUserId))
                .thenThrow(new DataAccessResourceFailureException("Synthetic test database outage"));
        var listener = new CustomerActivatedEventListener(mapper, unavailableDatabase);
        CountDownLatch attemptsObserved = new CountDownLatch(3);
        AtomicInteger attempts = new AtomicInteger();
        var container = startConsumer(listener, attemptsObserved, attempts);
        try {
            rabbit.convertAndSend(RabbitMqConfig.IDENTITY_EXCHANGE,
                    RabbitMqConfig.CUSTOMER_ACTIVATED_ROUTING_KEY, normalEvent);
            assertTrue(attemptsObserved.await(10, TimeUnit.SECONDS));
            var deadLetter = rabbit.receive(RabbitMqConfig.CUSTOMER_ACTIVATED_DLQ, 10000);
            assertNotNull(deadLetter, "An exhausted delivery must be broker-routed to the real Payment DLQ");
            assertEquals(normalEvent, RABBIT_CONFIG.jsonMessageConverter().fromMessage(deadLetter));
            assertNotNull(deadLetter.getMessageProperties().getHeaders().get("x-death"));
            assertEquals(3, attempts.get());
            verify(unavailableDatabase, times(3)).ensureWalletExists(normalUserId);
        } finally {
            container.stop();
            container.destroy();
        }
        assertEquals(0, admin.getQueueInfo(RabbitMqConfig.CUSTOMER_ACTIVATED_QUEUE).getMessageCount());
        assertWalletHttp200(normalUserId, new BigDecimal("85000.00"));
    }

    private SimpleMessageListenerContainer startConsumer(CustomerActivatedEventListener listener,
            CountDownLatch observed, AtomicInteger attempts) {
        RabbitProperties properties = new RabbitProperties();
        properties.getListener().getSimple().setAutoStartup(false);
        var factory = RABBIT_CONFIG.customerActivationListenerContainerFactory(
                new SimpleRabbitListenerContainerFactoryConfigurer(properties), connectionFactory,
                RABBIT_CONFIG.customerActivationRetryInterceptor());
        factory.setMessageConverter(RABBIT_CONFIG.jsonMessageConverter());
        var container = factory.createListenerContainer(null);
        container.setQueueNames(RabbitMqConfig.CUSTOMER_ACTIVATED_QUEUE);
        container.setConcurrentConsumers(1);
        container.setPrefetchCount(1);
        container.setMessageListener((MessageListener) message -> {
            if (attempts != null) {
                attempts.incrementAndGet();
                observed.countDown();
            }
            String payload = (String) RABBIT_CONFIG.jsonMessageConverter().fromMessage(message);
            listener.onCustomerActivated(payload);
            if (attempts == null) observed.countDown();
        });
        container.afterPropertiesSet();
        container.start();
        return container;
    }

    private void assertWalletHttp200(long userId, BigDecimal balance) throws Exception {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        var principal = new AuthenticatedUser(userId, "customer@example.test", authorities);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
        var mvc = MockMvcBuilders.standaloneSetup(new WalletController(databaseContext.getBean(WalletServiceImpl.class)))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/v1/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(balance.doubleValue()));
    }

    @Configuration
    @EnableRabbit
    static class AnnotationObservationConfiguration {

        @Bean
        static ListenerCompletionObserver listenerCompletionObserver() {
            return new ListenerCompletionObserver();
        }
    }

    static class ListenerCompletionObserver implements BeanPostProcessor, PriorityOrdered {

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (!(bean instanceof CustomerActivatedEventListener)) return bean;
            ProxyFactory proxy = new ProxyFactory(bean);
            proxy.setProxyTargetClass(true);
            proxy.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation -> {
                Object result = invocation.proceed();
                if ("onCustomerActivated".equals(invocation.getMethod().getName())) {
                    CountDownLatch observed = successfulAnnotationDeliveries;
                    if (observed != null) observed.countDown();
                }
                return result;
            });
            return proxy.getProxy();
        }
    }
}
