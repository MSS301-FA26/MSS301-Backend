package com.cinemaai.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinemaai.payment.controller.WalletController;
import com.cinemaai.payment.event.CustomerActivatedEvent;
import com.cinemaai.payment.exception.GlobalExceptionHandler;
import com.cinemaai.payment.listener.CustomerActivatedEventListener;
import com.cinemaai.payment.repository.CineWalletRepository;
import com.cinemaai.payment.repository.WalletTransactionRepository;
import com.cinemaai.payment.repository.WithdrawalRequestRepository;
import com.cinemaai.payment.security.AuthenticatedUser;
import com.cinemaai.payment.service.impl.WalletServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class WalletProvisioningPostgresIntegrationTest {

    private static final String SCHEMA = "payment_wallet_provision_it";
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbc;
    private CustomerActivatedEventListener listener;
    private ObjectMapper mapper;
    private WalletServiceImpl walletService;
    private SqlProbe sqlProbe;
    private MockMvc mvc;

    @BeforeAll
    static void setUpDatabaseAndContext() {
        DataSource dataSource = new DatabaseConfiguration().dataSource();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .schemas(SCHEMA).defaultSchema(SCHEMA).load().migrate();
        context = new AnnotationConfigApplicationContext(DatabaseConfiguration.class);
        jdbc = new JdbcTemplate(context.getBean(DataSource.class));
    }

    @AfterAll
    static void closeContext() {
        if (context != null) context.close();
    }

    @BeforeEach
    void setUp() {
        // This schema exists only in the required disposable wallet_payment_test database.
        jdbc.execute("TRUNCATE cine_wallets CASCADE");
        listener = context.getBean(CustomerActivatedEventListener.class);
        mapper = context.getBean(ObjectMapper.class);
        walletService = context.getBean(WalletServiceImpl.class);
        sqlProbe = context.getBean(SqlProbe.class);
        sqlProbe.reset();
        mvc = MockMvcBuilders.standaloneSetup(new WalletController(walletService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        authenticate(41L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customerActivationCreatesZeroWalletWithBothRequiredTimestamps() throws Exception {
        listener.onCustomerActivated(payload("customer-1", 41L));

        var wallet = context.getBean(CineWalletRepository.class).findByUserId(41L).orElseThrow();
        assertEquals(0, wallet.getBalance().compareTo(BigDecimal.ZERO));
        assertNotNull(wallet.getCreatedAt());
        assertNotNull(wallet.getUpdatedAt());
        assertEquals(1L, walletCount());
    }

    @Test
    void duplicateAndReplayEventsStillLeaveExactlyOneWallet() throws Exception {
        String original = payload("customer-1", 41L);
        listener.onCustomerActivated(original);
        var originalWallet = context.getBean(CineWalletRepository.class).findByUserId(41L).orElseThrow();
        listener.onCustomerActivated(original);
        listener.onCustomerActivated(payload("replay-1", 41L));

        var replayed = context.getBean(CineWalletRepository.class).findByUserId(41L).orElseThrow();
        assertEquals(1L, walletCount());
        assertEquals(originalWallet.getId(), replayed.getId());
        assertEquals(originalWallet.getCreatedAt(), replayed.getCreatedAt());
        assertEquals(originalWallet.getUpdatedAt(), replayed.getUpdatedAt());
    }

    @Test
    void provisioningAnExistingWalletNeverResetsItsBalanceOrTimestamps() throws Exception {
        listener.onCustomerActivated(payload("customer-1", 41L));
        jdbc.update("UPDATE cine_wallets SET balance = ? WHERE user_id = ?", new BigDecimal("85000.00"), 41L);
        var before = context.getBean(CineWalletRepository.class).findByUserId(41L).orElseThrow();

        listener.onCustomerActivated(payload("customer-replay", 41L));

        var after = context.getBean(CineWalletRepository.class).findByUserId(41L).orElseThrow();
        assertEquals(before.getId(), after.getId());
        assertEquals(new BigDecimal("85000.00"), after.getBalance());
        assertEquals(before.getCreatedAt(), after.getCreatedAt());
        assertEquals(before.getUpdatedAt(), after.getUpdatedAt());
        assertEquals(1L, walletCount());
    }

    @Test
    void provisionedWalletGetReturns200UsingAReadOnlySelectTransaction() throws Exception {
        listener.onCustomerActivated(payload("customer-1", 41L));
        long id = context.getBean(CineWalletRepository.class).findByUserId(41L).orElseThrow().getId();
        sqlProbe.reset();

        mvc.perform(get("/api/v1/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.balance").value(0));

        assertTrue(sqlProbe.sawSelect);
        assertTrue(sqlProbe.allSelectTransactionsReadOnly);
        assertFalse(sqlProbe.sawWrite);
        assertEquals(1L, walletCount());
    }

    @Test
    void missingWalletGetReturnsSafe404WithoutAnyInsert() throws Exception {
        mvc.perform(get("/api/v1/wallet"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet is not provisioned yet"));

        assertTrue(sqlProbe.sawSelect);
        assertTrue(sqlProbe.allSelectTransactionsReadOnly);
        assertFalse(sqlProbe.sawWrite);
        assertEquals(0L, walletCount());
    }

    @Test
    void concurrentDeliveriesAllSucceedWithExactlyOneWallet() throws Exception {
        int deliveries = 8;
        CountDownLatch ready = new CountDownLatch(deliveries);
        CountDownLatch start = new CountDownLatch(1);
        List<java.util.concurrent.Future<?>> attempts = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(deliveries)) {
            for (int index = 0; index < deliveries; index++) {
                String event = payload("customer-" + index, 41L);
                attempts.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test barrier timed out");
                    listener.onCustomerActivated(event);
                    return null;
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            for (var attempt : attempts) attempt.get(15, TimeUnit.SECONDS);
        } finally {
            start.countDown();
        }
        assertEquals(1L, walletCount());
        assertEquals(0, walletService.getWallet(41L).balance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void realIdentityOtpAndFirstGoogleActivationEventsProvisionWalletsAndGetReturns200() throws Exception {
        Path fixtures = Path.of(System.getProperty("wallet.test.event-fixtures",
                "../identity-service/target/customer-activation-events.json"));
        var events = mapper.readTree(Files.readString(fixtures));
        for (String flow : List.of("normal", "google")) {
            var event = events.get(flow);
            assertNotNull(event, "Identity integration fixture must contain " + flow);
            long userId = event.get("userId").longValue();
            assertTrue(userId > 0);
            listener.onCustomerActivated(event.toString());
            authenticate(userId);
            mvc.perform(get("/api/v1/wallet"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balance").value(0));
        }
        assertEquals(2L, walletCount());
    }

    private String payload(String eventId, long userId) throws Exception {
        return mapper.writeValueAsString(new CustomerActivatedEvent(eventId, userId,
                LocalDateTime.of(2026, 10, 11, 0, 0), "integration-1"));
    }

    private long walletCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM cine_wallets", Long.class);
    }

    private void authenticate(long userId) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        var principal = new AuthenticatedUser(userId, "customer@example.test", authorities);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    @EnableJpaRepositories(basePackageClasses = CineWalletRepository.class)
    static class DatabaseConfiguration {

        @Bean
        DataSource dataSource() {
            String url = requiredProperty("wallet.test.jdbc-url");
            URI uri = URI.create(url.substring("jdbc:".length()));
            if (!url.startsWith("jdbc:postgresql://") || !"127.0.0.1".equals(uri.getHost())
                    || !"/wallet_payment_test".equals(uri.getPath()) || uri.getPort() == 5432
                    || uri.getPort() <= 0 || uri.getQuery() != null) {
                throw new IllegalArgumentException("Use only an isolated loopback wallet_payment_test database on a non-production port");
            }
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl(url + "?currentSchema=" + SCHEMA);
            dataSource.setUsername(requiredProperty("wallet.test.username"));
            dataSource.setPassword(requiredProperty("wallet.test.password"));
            return dataSource;
        }

        @Bean
        SqlProbe sqlProbe() {
            return new SqlProbe();
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, SqlProbe sqlProbe) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.cinemaai.payment.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(java.util.Map.of(
                    "hibernate.hbm2ddl.auto", "validate",
                    "hibernate.session_factory.statement_inspector", sqlProbe));
            return factory;
        }

        @Bean
        JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        WalletServiceImpl walletService(CineWalletRepository wallets, WalletTransactionRepository transactions,
                WithdrawalRequestRepository withdrawals) {
            return new WalletServiceImpl(wallets, transactions, withdrawals);
        }

        @Bean
        WalletProvisioningService walletProvisioningService(CineWalletRepository wallets) {
            return new WalletProvisioningService(wallets);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        CustomerActivatedEventListener listener(ObjectMapper mapper, WalletProvisioningService provisioningService) {
            return new CustomerActivatedEventListener(mapper, provisioningService);
        }

        private static String requiredProperty(String key) {
            String value = System.getProperty(key);
            if (value == null) throw new IllegalArgumentException("Required integration test property: " + key);
            return value;
        }
    }

    static class SqlProbe implements StatementInspector {
        private volatile boolean sawSelect;
        private volatile boolean sawWrite;
        private volatile boolean allSelectTransactionsReadOnly;

        @Override
        public String inspect(String sql) {
            String normalized = sql.stripLeading().toLowerCase(java.util.Locale.ROOT);
            if (normalized.startsWith("select")) {
                sawSelect = true;
                if (!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                    allSelectTransactionsReadOnly = false;
                }
            } else if (normalized.startsWith("insert") || normalized.startsWith("update")
                    || normalized.startsWith("delete")) {
                sawWrite = true;
            }
            return sql;
        }

        void reset() {
            sawSelect = false;
            sawWrite = false;
            allSelectTransactionsReadOnly = true;
        }
    }
}
