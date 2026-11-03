package com.cinemaai.payment.security;

import com.cinemaai.payment.controller.AdminLoyaltyController;
import com.cinemaai.payment.service.impl.LoyaltyServiceImpl;
import com.cinemaai.payment.client.IdentityClient;
import com.cinemaai.payment.mapper.LoyaltyMapper;
import com.cinemaai.payment.repository.*;
import javax.sql.DataSource;
import java.util.*;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminLoyaltyController.class)
@Import({SecurityConfig.class,LoyaltyServiceImpl.class,AdminLoyaltyDatabaseIntegrationTest.Database.class})
@TestPropertySource(properties={"APPLICATION_NAME=loyalty-db-test","server.address=127.0.0.1","server.port=0","app.gateway.secret=test","app.internal.secret=test","app.jwt.secret=01234567890123456789012345678901"})
@EnabledIfSystemProperty(named="authorization.payment.url",matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/payment_auth")
class AdminLoyaltyDatabaseIntegrationTest {
    @TestConfiguration @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses=LoyaltyPointRepository.class)
    static class Database {
        @Bean DataSource dataSource() {
            String url=System.getProperty("authorization.payment.url");
            if(url==null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:(?!5432/)[0-9]+/payment_auth")) throw new IllegalArgumentException("Disposable loopback database required");
            return new DriverManagerDataSource(url,"coverage_user","coverage_pass");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource d) {
            var f=new LocalContainerEntityManagerFactoryBean(); f.setDataSource(d);
            f.setPackagesToScan("com.cinemaai.payment.entity"); f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","create-drop")); return f;
        }
        @Bean JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory f) { return new JpaTransactionManager(f); }
        @Bean LoyaltyMapper loyaltyMapper() { return org.mapstruct.factory.Mappers.getMapper(LoyaltyMapper.class); }
    }
    @Autowired MockMvc mvc;
    @Autowired LoyaltyPointRepository points;
    @Autowired LoyaltyPointTransactionRepository history;
    @Autowired LoyaltyConfigurationRepository configurations;
    @MockBean IdentityClient identity;
    @BeforeEach void clear() {
        history.deleteAll(); points.deleteAll(); configurations.deleteAll();
        org.mockito.Mockito.when(identity.getUserProfile(99L)).thenReturn(new IdentityClient.UserProfileDto(99L,"target@test.invalid","Target","0900"));
    }
    org.springframework.http.HttpHeaders headers(String role) {
        var h=new org.springframework.http.HttpHeaders(); h.set("X-Gateway-Secret","test"); h.set("X-User-Roles","ADMIN");
        if(role!=null) h.setBearerAuth(Jwts.builder().subject("test").claim("userId",7L).claim("roles",List.of(role)).claim("cinemaId",202L)
                .expiration(new Date(System.currentTimeMillis()+60000))
                .signWith(Keys.hmacShaKeyFor("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8))).compact());
        return h;
    }
    @Test void adminWritesPersistExactBalancesHistoryAndConfiguration() throws Exception {
        mvc.perform(put("/api/v1/admin/loyalty/config").headers(headers("ADMIN")).contentType("application/json")
                .content("{\"cinemaId\":101,\"earningRatePercent\":2,\"redemptionPoints\":1000,\"redemptionValueVnd\":1000,\"expiryMonth\":12,\"expiryDay\":31}"))
                .andExpect(status().isOk());
        assertEquals(0,configurations.findByCinemaId(101L).orElseThrow().getEarningRatePercent().compareTo(new java.math.BigDecimal("2")));
        for(String cinema:List.of("","\"cinemaId\":202,")) {
            mvc.perform(put("/api/v1/admin/loyalty/config").headers(headers("ADMIN")).contentType("application/json")
                    .content("{"+cinema+"\"earningRatePercent\":2,\"redemptionPoints\":1000,\"redemptionValueVnd\":1000,\"expiryMonth\":12,\"expiryDay\":31}"))
                    .andExpect(status().isOk());
        }
        assertEquals(3,configurations.count());
        for(String query:List.of("","?cinemaId=101","?cinemaId=202"))
            mvc.perform(get("/api/v1/admin/loyalty/config"+query).headers(headers("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/loyalty/add").headers(headers("ADMIN")).contentType("application/json")
                .content("{\"userId\":99,\"points\":100}")).andExpect(status().isOk());
        assertEquals(100,points.findByUserId(99L).orElseThrow().getPoints()); assertEquals(1,history.count());
        mvc.perform(post("/api/v1/admin/loyalty/99/redeem").headers(headers("ADMIN")).param("points","30")).andExpect(status().isOk());
        assertEquals(70,points.findByUserId(99L).orElseThrow().getPoints()); assertEquals(2,history.count());
        mvc.perform(post("/api/v1/admin/loyalty/99/redeem").headers(headers("ADMIN")).param("points","1000")).andExpect(status().isBadRequest());
        assertEquals(70,points.findByUserId(99L).orElseThrow().getPoints()); assertEquals(2,history.count());
        mvc.perform(post("/api/v1/admin/loyalty/points/expire-all-active").headers(headers("ADMIN"))).andExpect(status().isOk());
        assertEquals(0,points.findByUserId(99L).orElseThrow().getPoints()); assertEquals(3,history.count());
        assertTrue(history.findAll().stream().allMatch(t -> t.getUserId().equals(99L)));
        for(String suffix:List.of("config","transactions","report"))
            mvc.perform(get("/api/v1/admin/loyalty/"+suffix).headers(headers("ADMIN"))).andExpect(status().isOk());
    }
    @Test void deniedRequestsAndInvalidTargetsHaveNoDatabaseSideEffects() throws Exception {
        for(String role:List.of("USER","CUSTOMER","STAFF","MANAGER")) {
            mvc.perform(put("/api/v1/admin/loyalty/config").headers(headers(role)).contentType("application/json")
                    .content("{\"cinemaId\":101,\"earningRatePercent\":2,\"redemptionPoints\":1000,\"redemptionValueVnd\":1000,\"expiryMonth\":12,\"expiryDay\":31}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/admin/loyalty/points/expire-all-active").headers(headers(role))).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/admin/loyalty/add").headers(headers(role)).contentType("application/json")
                    .content("{\"userId\":99,\"points\":100}")).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/admin/loyalty/99/redeem").headers(headers(role)).param("points","1")).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/admin/loyalty/add").headers(headers(null)).contentType("application/json")
                .content("{\"userId\":99,\"points\":100}")).andExpect(status().isUnauthorized());
        var invalid=headers(null); invalid.setBearerAuth("invalid");
        mvc.perform(post("/api/v1/admin/loyalty/add").headers(invalid).contentType("application/json")
                .content("{\"userId\":99,\"points\":100}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/loyalty/add").headers(headers("ADMIN")).contentType("application/json")
                .content("{\"points\":100}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/loyalty/add").headers(headers("ADMIN")).contentType("application/json")
                .content("{\"userId\":-1,\"points\":100}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/loyalty/999/redeem").headers(headers("ADMIN")).param("points","1")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/loyalty/add").headers(headers("ADMIN")).contentType("application/json")
                .content("{\"userId\":999,\"points\":100}")).andExpect(status().isNotFound());
        assertEquals(0,points.count()); assertEquals(0,history.count()); assertEquals(0,configurations.count());
    }

    @Test void managerConfigurationUsesCurrentIdentityScopeAndPersistsOnlyAssignedCinema() throws Exception {
        org.mockito.Mockito.when(identity.getUserAccessScope(7L)).thenReturn(new IdentityClient.UserAccessScope(7L,"ACTIVE",List.of("MANAGER"),101L));
        for(String query:List.of("","?cinemaId=101"))
            mvc.perform(get("/api/v1/admin/loyalty/config"+query).headers(headers("MANAGER")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.cinemaId").value(101));
        String config="{\"cinemaId\":101,\"earningRatePercent\":3,\"redemptionPoints\":1000,\"redemptionValueVnd\":1000,\"expiryMonth\":12,\"expiryDay\":31}";
        mvc.perform(put("/api/v1/admin/loyalty/config").headers(headers("MANAGER")).contentType("application/json").content(config)).andExpect(status().isOk());
        assertEquals(0,configurations.findByCinemaId(101L).orElseThrow().getEarningRatePercent().compareTo(new java.math.BigDecimal("3")));
        mvc.perform(get("/api/v1/admin/loyalty/config?cinemaId=202").headers(headers("MANAGER"))).andExpect(status().isForbidden());
        for(String denied:List.of(config.replace("101","202"),config.replace("\"cinemaId\":101,","")))
            mvc.perform(put("/api/v1/admin/loyalty/config").headers(headers("MANAGER")).contentType("application/json").content(denied)).andExpect(status().isForbidden());
        for(String suffix:List.of("transactions","report"))
            mvc.perform(get("/api/v1/admin/loyalty/"+suffix).headers(headers("MANAGER"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/loyalty/add").headers(headers("MANAGER")).contentType("application/json").content("{\"userId\":99,\"points\":100}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/loyalty/99/redeem").headers(headers("MANAGER")).param("points","1")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/loyalty/points/expire-all-active").headers(headers("MANAGER"))).andExpect(status().isForbidden());
        assertEquals(1,configurations.count()); assertEquals(0,points.count()); assertEquals(0,history.count());
        assertTrue(configurations.findByCinemaIdIsNull().isEmpty()); assertTrue(configurations.findByCinemaId(202L).isEmpty());
        var failures=Arrays.asList(null,new IdentityClient.UserAccessScope(7L,"ACTIVE",List.of("MANAGER"),null),
                new IdentityClient.UserAccessScope(7L,"ACTIVE",List.of("CUSTOMER"),101L),
                new IdentityClient.UserAccessScope(7L,"DISABLED",List.of("MANAGER"),101L),
                new IdentityClient.UserAccessScope(8L,"ACTIVE",List.of("MANAGER"),101L));
        for(var scope:failures) {
            org.mockito.Mockito.when(identity.getUserAccessScope(7L)).thenReturn(scope);
            mvc.perform(get("/api/v1/admin/loyalty/config").headers(headers("MANAGER"))).andExpect(status().isForbidden());
            mvc.perform(put("/api/v1/admin/loyalty/config").headers(headers("MANAGER")).contentType("application/json").content(config.replace(":3,",":4,"))).andExpect(status().isForbidden());
        }
        org.mockito.Mockito.when(identity.getUserAccessScope(7L)).thenThrow(new org.springframework.web.client.ResourceAccessException("timeout"));
        mvc.perform(put("/api/v1/admin/loyalty/config").headers(headers("MANAGER")).contentType("application/json").content(config)).andExpect(status().isForbidden());
        assertEquals(0,configurations.findByCinemaId(101L).orElseThrow().getEarningRatePercent().compareTo(new java.math.BigDecimal("3")));
        assertEquals(1,configurations.count()); assertEquals(0,points.count()); assertEquals(0,history.count());
    }
}
