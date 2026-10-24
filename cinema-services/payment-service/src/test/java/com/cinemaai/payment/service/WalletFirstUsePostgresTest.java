package com.cinemaai.payment.service;

import com.cinemaai.payment.dto.request.*;
import com.cinemaai.payment.dto.response.WalletResponse;
import com.cinemaai.payment.mapper.WalletMapper;
import com.cinemaai.payment.repository.*;
import com.cinemaai.payment.service.impl.WalletServiceImpl;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import static org.junit.jupiter.api.Assertions.*;

/** Real Spring transaction proxy/JPA/PostgreSQL. No database or service mocks. */
@EnabledIfSystemProperty(named="wallet.test.url", matches="jdbc:postgresql://127\\.0\\.0\\.1:57104/payment_db")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WalletFirstUsePostgresTest {
    static final String SCHEMA="wallet_first_use_"+Long.toUnsignedString(System.nanoTime());
    AnnotationConfigApplicationContext context;
    JdbcTemplate jdbc;
    WalletService service;
    @BeforeAll void start() {
        var ds=new Config().dataSource();
        Flyway.configure().dataSource(ds).schemas(SCHEMA).defaultSchema(SCHEMA).locations("classpath:db/migration").load().migrate();
        context=new AnnotationConfigApplicationContext(Config.class);
        jdbc=new JdbcTemplate(context.getBean(DataSource.class)); service=context.getBean(WalletService.class);
    }
    @AfterAll void stop(){ if(context!=null)context.close(); }
    @BeforeEach void reset(){ jdbc.execute("TRUNCATE cine_wallets CASCADE"); }
    int count(String table){return jdbc.queryForObject("select count(*) from "+table,Integer.class);}
    @Test void firstUseAndRepeatedReadsCreateOneZeroWalletWithoutFinancialSideEffects() {
        WalletResponse first=service.getWallet(77L),again=service.getWallet(77L);
        assertEquals(first.id(),again.id());
        assertEquals(77L,jdbc.queryForObject("select user_id from cine_wallets where id=?",Long.class,first.id()));
        assertEquals(0,BigDecimal.ZERO.compareTo(first.balance()));
        assertEquals(0,first.balance().compareTo(again.balance()));
        assertEquals(1,count("cine_wallets")); assertEquals(0,count("wallet_transactions")); assertEquals(0,count("withdrawal_requests"));
    }
    @Test void existingWalletReadDoesNotChangeBalanceOrTimestamps() {
        jdbc.update("insert into cine_wallets(user_id,balance,created_at,updated_at) values (77,123456,current_timestamp,current_timestamp)");
        var before=jdbc.queryForMap("select * from cine_wallets where user_id=77");
        assertEquals(0,new BigDecimal("123456").compareTo(service.getWallet(77L).balance()));
        assertEquals(before,jdbc.queryForMap("select * from cine_wallets where user_id=77"));
        assertEquals(0,count("wallet_transactions"));
    }
    @Test void eightConcurrentFirstUsesCommitOneLogicalWallet() throws Exception {
        var pool=Executors.newFixedThreadPool(8); var ready=new CountDownLatch(8); var go=new CountDownLatch(1);
        var futures=new ArrayList<Future<WalletResponse>>();
        try {
            for(int i=0;i<8;i++)futures.add(pool.submit(()->{ready.countDown(); assertTrue(go.await(10,TimeUnit.SECONDS)); return service.getWallet(77L);}));
            assertTrue(ready.await(10,TimeUnit.SECONDS)); go.countDown(); var ids=new HashSet<Long>();
            for(var future:futures){var wallet=future.get(30,TimeUnit.SECONDS);ids.add(wallet.id());assertEquals(0,wallet.balance().compareTo(BigDecimal.ZERO));}
            assertEquals(1,ids.size()); assertEquals(1,count("cine_wallets")); assertEquals(0,count("wallet_transactions")); assertEquals(0,count("withdrawal_requests"));
        } finally {go.countDown();pool.shutdownNow();}
    }
    @Test void creditIdempotencyAndWithdrawalRejectApproveRemainIntact() {
        service.getWallet(77L);
        var credit=new CreditWalletInternalRequest(77L,new BigDecimal("50000"),12L,"TEST-12","Test refund");
        service.creditWalletInternal(credit);service.creditWalletInternal(credit);
        assertEquals(1,count("wallet_transactions"));
        var request=new WithdrawalCreateRequest(new BigDecimal("10000"),"TEST","000","Fixture",null);
        var rejected=service.createWithdrawalRequest(77L,request);
        assertEquals(0,new BigDecimal("40000").compareTo(service.getWallet(77L).balance()));
        service.rejectWithdrawal(rejected.id(),null);
        assertEquals(0,new BigDecimal("50000").compareTo(service.getWallet(77L).balance()));
        var paid=service.createWithdrawalRequest(77L,request);service.approveWithdrawal(paid.id(),null);
        assertEquals(0,new BigDecimal("40000").compareTo(service.getWallet(77L).balance()));
        assertEquals(1,count("cine_wallets")); assertEquals(5,count("wallet_transactions")); assertEquals(2,count("withdrawal_requests"));
    }
    @Configuration @EnableTransactionManagement @EnableJpaRepositories(basePackageClasses=CineWalletRepository.class)
    static class Config {
        @Bean DataSource dataSource(){
            String url=System.getProperty("wallet.test.url");
            if(!"jdbc:postgresql://127.0.0.1:57104/payment_db".equals(url))throw new IllegalArgumentException("Owned disposable database required");
            return new DriverManagerDataSource(url+"?currentSchema="+SCHEMA,"payment_verify",System.getProperty("wallet.test.password"));
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource d){var f=new LocalContainerEntityManagerFactoryBean();f.setDataSource(d);f.setPackagesToScan("com.cinemaai.payment.entity");f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));return f;}
        @Bean JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory f){return new JpaTransactionManager(f);}
        @Bean WalletMapper walletMapper(){return org.mapstruct.factory.Mappers.getMapper(WalletMapper.class);}
        @Bean WalletService walletService(CineWalletRepository w,WalletTransactionRepository t,WithdrawalRequestRepository r,WalletMapper m){return new WalletServiceImpl(w,t,r,m);}
    }
}
