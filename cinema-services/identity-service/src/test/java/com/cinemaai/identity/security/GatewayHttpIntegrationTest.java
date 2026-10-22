package com.cinemaai.identity.security;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes=GatewayHttpIntegrationTest.Config.class, properties={"APPLICATION_NAME=http-test", "management.endpoints.web.exposure.include=health,info", "management.endpoint.health.show-details=never", "springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false", "server.address=127.0.0.1", "LOG_LEVEL_PATTERN=%5p", "app.mail.enabled=false", "app.gateway.secret=gateway-test", "app.internal.secret=internal-test", "app.jwt.secret=01234567890123456789012345678901", "app.jwt.access-expiration-ms=60000", "app.jwt.refresh-expiration-ms=60000"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GatewayHttpIntegrationTest {
 @LocalServerPort int downstreamPort; @Autowired JwtService jwt; @MockBean CustomUserDetailsService users; GatewayHttpHarness gateway;
 @BeforeAll void start() throws Exception {gateway=new GatewayHttpHarness(downstreamPort);}
 @AfterAll void stop() throws Exception {if(gateway!=null)gateway.close();}
 @Test void publicAndAuthenticatedRoutesThroughGateway() throws Exception {
  when(users.loadUserByUsername("a@example.test")).thenReturn(User.withUsername("a@example.test").password("x").roles("USER").build());
  assertEquals(200,gateway.send("/api/v1/auth/probe",Map.of()).statusCode());
  assertEquals(401,gateway.send("/api/v1/users/probe",Map.of()).statusCode());
  String token=jwt.generateAccessToken(7L,"a@example.test",List.of("USER"));
  var result=gateway.send("/api/v1/users/probe",Map.of("Authorization","Bearer "+token,"X-User-Id","9","X-User-Roles","ADMIN"));
  assertEquals(200,result.statusCode()); assertEquals("a@example.test:[ROLE_USER]",result.body());
  assertEquals(403,gateway.send("/api/v1/admin/users",Map.of("Authorization","Bearer "+token,"X-User-Roles","ADMIN")).statusCode());
 }
 @Configuration @EnableAutoConfiguration(excludeName={"org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration", "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration"}, exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,JpaRepositoriesAutoConfiguration.class,FlywayAutoConfiguration.class})
 @Import({SecurityConfig.class,JwtAuthenticationFilter.class,JwtService.class,Probe.class}) static class Config { }
 @RestController static class Probe {
  @GetMapping("/api/v1/auth/probe") String pub(){return "ok";}
  @GetMapping("/api/v1/users/probe") String user(Authentication a){return a.getName()+":"+a.getAuthorities();}
  @GetMapping("/api/v1/admin/users") String admin(){return "ok";}
 }
}
