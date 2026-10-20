package com.cinemaai.payment.service;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.enums.PaymentProvider;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.mapper.PaymentMapper;
import com.cinemaai.payment.repository.OutboxEventRepository;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.service.impl.PaymentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.flywaydb.core.Flyway;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Real Spring proxy, JPA repository and PostgreSQL; BookingClient is the only boundary double. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "payment.service.test.jdbc-url", matches = ".+")
class PaymentServicePostgresConcurrencyIntegrationTest {
    static final String SCHEMA = "payment_service_it";
    AnnotationConfigApplicationContext context;
    JdbcTemplate jdbc;
    PaymentRepository payments;
    BookingClient bookingClient;
    PaymentService service;

    @BeforeAll void start() {
        DataSource ds = new TestConfig().dataSource();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").schemas(SCHEMA).defaultSchema(SCHEMA).load().migrate();
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        jdbc = new JdbcTemplate(context.getBean(DataSource.class));
        payments = context.getBean(PaymentRepository.class);
        bookingClient = context.getBean(BookingClient.class);
        service = context.getBean(PaymentService.class);
    }
    @AfterAll void stop() { if (context != null) context.close(); }
    @BeforeEach void clear() { jdbc.execute("TRUNCATE payments, outbox_events CASCADE"); }

    @Test void t01NormalCreate() {
        stubBooking(10L, 1L, "PENDING_PAYMENT", 100000, "BK10");
        service.createVnpayPayment(1L, 10L, null, "127.0.0.1");
        Payment p = payments.findByBookingId(10L).orElseThrow();
        assertAll(() -> assertEquals(PaymentStatus.PENDING, p.getStatus()), () -> assertEquals(0, p.getAmount().compareTo(BigDecimal.valueOf(100000))), () -> assertNotNull(p.getId()));
    }

@Test
void t11FailedThenSuccessRejectsAnotherPayment() {
    stubBooking(40L, 1L, "PENDING_PAYMENT", 100000, "BK40");

    payments.saveAndFlush(Payment.builder()
            .bookingId(40L)
            .userId(1L)
            .provider(PaymentProvider.VNPAY)
            .amount(BigDecimal.valueOf(100000))
            .status(PaymentStatus.FAILED)
            .build());

    payments.saveAndFlush(Payment.builder()
            .bookingId(40L)
            .userId(1L)
            .provider(PaymentProvider.VNPAY)
            .amount(BigDecimal.valueOf(100000))
            .status(PaymentStatus.SUCCESS)
            .build());

    assertThrows(
            BadRequestException.class,
            () -> service.createVnpayPayment(1L, 40L, null, "127.0.0.1")
    );

    assertEquals(2, payments.findAll().size());

    assertTrue(
            payments.findByBookingIdAndStatus(
                    40L,
                    PaymentStatus.PENDING
            ).isEmpty()
    );
}

    @Test void t02TwoConcurrentCreates() throws Exception { stubBooking(10L,1L,"PENDING_PAYMENT",100000,"BK10"); assertEquals(1, concurrentBooking(10L, 2)); }
    @Test void t03TenConcurrentCreates() throws Exception { stubBooking(11L,1L,"PENDING_PAYMENT",100000,"BK11"); assertEquals(1, concurrentBooking(11L, 10)); }
    @Test void t04ExistingPendingIsReused() { stubBooking(12L,1L,"PENDING_PAYMENT",100000,"BK12"); service.createVnpayPayment(1L,12L,null,"127.0.0.1"); service.createVnpayPayment(1L,12L,null,"127.0.0.1"); assertEquals(1, payments.findAll().size()); }
    @Test void t05ExistingSuccessIsRejected() { stubBooking(13L,1L,"PENDING_PAYMENT",100000,"BK13"); payments.save(Payment.builder().bookingId(13L).userId(1L).provider(PaymentProvider.VNPAY).amount(BigDecimal.valueOf(100000)).status(PaymentStatus.SUCCESS).build()); assertThrows(RuntimeException.class, () -> service.createVnpayPayment(1L,13L,null,"127.0.0.1")); }
    @Test void t06FailedRetryCreatesNewPending() { stubBooking(14L,1L,"PENDING_PAYMENT",100000,"BK14"); payments.save(Payment.builder().bookingId(14L).userId(1L).provider(PaymentProvider.VNPAY).amount(BigDecimal.valueOf(100000)).status(PaymentStatus.FAILED).build()); service.createVnpayPayment(1L,14L,null,"127.0.0.1"); assertEquals(1, payments.findByBookingIdAndStatus(14L,PaymentStatus.PENDING).stream().count()); }
    @Test void t07ConcurrentFailedRetry() throws Exception { stubBooking(15L,1L,"PENDING_PAYMENT",100000,"BK15"); payments.save(Payment.builder().bookingId(15L).userId(1L).provider(PaymentProvider.VNPAY).amount(BigDecimal.valueOf(100000)).status(PaymentStatus.FAILED).build()); assertEquals(1, concurrentBooking(15L, 2)); }
    @Test void t08RollbackLeavesSingleRow() throws Exception { stubBooking(16L,1L,"PENDING_PAYMENT",100000,"BK16"); assertEquals(1, concurrentBooking(16L, 10)); assertEquals(1, payments.findByBookingIdAndStatus(16L,PaymentStatus.PENDING).stream().count()); }
    @Test void t09FoodOrderConcurrency() throws Exception { stubFood(20L,1L,50000,"FOOD20"); assertEquals(1, concurrentFood(20L, 10)); }
    @Test void t10DifferentBookingsProceedConcurrently() throws Exception { ExecutorService e=Executors.newFixedThreadPool(4); List<Future<?>> fs=new ArrayList<>(); for(long id=30;id<34;id++){ final long bookingId=id; stubBooking(bookingId,1L,"PENDING_PAYMENT",100000,"BK"+bookingId); fs.add(e.submit(()->service.createVnpayPayment(1L,bookingId,null,"127.0.0.1")));} for(Future<?> f:fs)f.get(10,TimeUnit.SECONDS); e.shutdownNow(); assertEquals(4, payments.findAll().size()); }

    int concurrentBooking(long id,int n) throws Exception { return concurrent(() -> service.createVnpayPayment(1L,id,null,"127.0.0.1"),n); }
    int concurrentFood(long id,int n) throws Exception { return concurrent(() -> service.createVnpayPayment(1L,null,id,"127.0.0.1"),n); }
    int concurrent(Callable<?> call,int n) throws Exception { ExecutorService e=Executors.newFixedThreadPool(n); CountDownLatch gate=new CountDownLatch(1); List<Future<?>> fs=new ArrayList<>(); for(int i=0;i<n;i++)fs.add(e.submit(()->{gate.await(); try{call.call();return true;}catch(Exception x){return x;}})); gate.countDown(); int ok=0; for(Future<?> f:fs){Object result=f.get(15,TimeUnit.SECONDS); if(Boolean.TRUE.equals(result))ok++; else if(result instanceof Exception x) System.out.println("concurrent business result: "+x.getClass().getSimpleName()+" "+x.getMessage());} e.shutdownNow(); return jdbc.queryForObject("select count(*) from payments where status='PENDING'",Integer.class)==1?1:0; }
    void stubBooking(long id,long user,String status,int amount,String code){ ((FakeBookingClient)bookingClient).booking=new BookingClient.BookingInfo(id,code,user,BigDecimal.valueOf(amount),status,LocalDateTime.now().plusMinutes(2),0,1L); }
    void stubFood(long id,long user,int amount,String code){ ((FakeBookingClient)bookingClient).food=new BookingClient.FoodOrderInfo(id,code,user,BigDecimal.valueOf(amount),"PENDING"); }

    @Configuration @EnableTransactionManagement(proxyTargetClass=true) @EnableJpaRepositories(basePackageClasses=PaymentRepository.class)
    static class TestConfig {
        @Bean DataSource dataSource(){ String u=System.getProperty("payment.service.test.jdbc-url"); if(u==null)throw new IllegalArgumentException("payment.service.test.jdbc-url required"); URI uri=URI.create(u.substring(5)); if(!u.startsWith("jdbc:postgresql://127.0.0.1/") && !(u.startsWith("jdbc:postgresql://127.0.0.1:") && uri.getPort()!=5432))throw new IllegalArgumentException("loopback disposable PostgreSQL required"); DriverManagerDataSource d=new DriverManagerDataSource(u+"?currentSchema="+SCHEMA); d.setUsername(System.getProperty("payment.service.test.username")); d.setPassword(System.getProperty("payment.service.test.password")); return d; }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource d){var f=new LocalContainerEntityManagerFactoryBean();f.setDataSource(d);f.setPackagesToScan("com.cinemaai.payment.entity");f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));return f;}
        @Bean JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory f){return new JpaTransactionManager(f);}
        @Bean BookingClient bookingClient(){return new FakeBookingClient();}
        @Bean LoyaltyService loyaltyService(){return (LoyaltyService) java.lang.reflect.Proxy.newProxyInstance(LoyaltyService.class.getClassLoader(),new Class[]{LoyaltyService.class},(p,m,a)->m.getReturnType()==int.class?0:null);}
        @Bean OutboxPublisherWorker outboxPublisherWorker(){return Mockito.mock(OutboxPublisherWorker.class);}
        @Bean ObjectMapper objectMapper(){return new ObjectMapper().findAndRegisterModules();}
        @Bean PaymentMapper paymentMapper(){return org.mapstruct.factory.Mappers.getMapper(PaymentMapper.class);}
        @Bean PropertySourcesPlaceholderConfigurer properties(){var p=new Properties();p.put("vnpay.tmn-code","TEST");p.put("vnpay.hash-secret","TEST");p.put("vnpay.pay-url","https://example.invalid/pay");p.put("vnpay.return-url","https://example.invalid/return");p.put("booking.service.url","http://127.0.0.1:1");p.put("app.internal.secret","TEST");var c=new PropertySourcesPlaceholderConfigurer();c.setProperties(p);return c;}
        @Bean PaymentServiceImpl paymentService(PaymentRepository p,OutboxEventRepository o,OutboxPublisherWorker w,BookingClient b,LoyaltyService l,ObjectMapper m,PaymentMapper pm){return new PaymentServiceImpl(p,o,w,b,l,m,pm);}
    }
    static class FakeBookingClient extends BookingClient {
        volatile BookingInfo booking; volatile FoodOrderInfo food;
        FakeBookingClient(){super("http://127.0.0.1:1","TEST");}
        @Override public BookingInfo getBooking(Long id){return booking;}
        @Override public FoodOrderInfo getFoodOrder(Long id){return food;}
    }
}
