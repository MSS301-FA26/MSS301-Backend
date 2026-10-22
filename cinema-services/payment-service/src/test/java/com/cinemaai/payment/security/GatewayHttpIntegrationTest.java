package com.cinemaai.payment.security;
import com.cinemaai.payment.controller.PaymentController;
import com.cinemaai.payment.service.PaymentService;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes=GatewayHttpIntegrationTest.Config.class, properties={
 "APPLICATION_NAME=http-test", "management.endpoints.web.exposure.include=health,info", "management.endpoint.health.show-details=never", "springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false", "server.address=127.0.0.1", "app.gateway.secret=gateway-test", "app.internal.secret=internal-test",
 "app.jwt.secret=01234567890123456789012345678901", "vnpay.success-redirect-url=https://client.example.test/result", "LOG_LEVEL_PATTERN=%5p"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GatewayHttpIntegrationTest {
 @LocalServerPort int downstreamPort;
 GatewayHttpHarness gateway;
 @BeforeAll void start() throws Exception { gateway = new GatewayHttpHarness(downstreamPort); }
 @AfterAll void stop() throws Exception { if (gateway != null) gateway.close(); }
 String token(String key) { return Jwts.builder().subject("a@example.test").claim("userId",7L).claim("roles",List.of("USER"))
  .expiration(new Date(System.currentTimeMillis()+60000)).signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact(); }
 @Test void jwtAndSpoofedHeadersAcrossHttp() throws Exception {
  var good = "Bearer " + token("01234567890123456789012345678901");
  var bad = "Bearer " + token("abcdefghijklmnopqrstuvwxyz123456");
  assertEquals(401,gateway.send("/api/v1/payments/probe",Map.of()).statusCode());
  assertEquals(401,gateway.send("/api/v1/payments/probe",Map.of("Authorization",bad)).statusCode());
  var response=gateway.send("/api/v1/payments/probe",Map.of("Authorization",good,"X-User-Id","9","X-User-Roles","ADMIN","X-User-Email","fake","X-User-Cinema-Id","2","X-Internal-Service-Secret","fake"));
  assertEquals(200,response.statusCode()); assertEquals("7:[ROLE_USER]:null:null:null:null:null",response.body());
 }
 @Test void internalDeniedViaGatewayButSecretWorksDirect() throws Exception {
  assertEquals(404,gateway.send("/internal/probe",Map.of("X-Internal-Service-Secret","internal-test")).statusCode());
  assertEquals(403,gateway.sendTo(downstreamPort,"/internal/probe",Map.of()).statusCode());
  assertEquals(403,gateway.sendTo(downstreamPort,"/internal/probe",Map.of("X-Internal-Service-Secret","wrong")).statusCode());
  assertEquals(200,gateway.sendTo(downstreamPort,"/internal/probe",Map.of("X-Internal-Service-Secret","internal-test")).statusCode());
 }
 @MockBean PaymentService paymentService;
 @Test void vnpayPublicGatewayRoutingUsesRealController() throws Exception {
  when(paymentService.processVnpayIpn(anyMap())).thenReturn(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
  var ipn = gateway.send("/api/v1/payments/vnpay/ipn?vnp_TxnRef=42", Map.of());
  assertEquals(200, ipn.statusCode()); assertTrue(ipn.body().contains("97"));
  var ret = gateway.send("/api/v1/payments/vnpay/return?vnp_TxnRef=42", Map.of());
  assertEquals(302, ret.statusCode()); assertEquals("https://client.example.test/result?vnp_TxnRef=42", ret.headers().firstValue("Location").orElseThrow());
  verify(paymentService, times(2)).processVnpayIpn(anyMap());
 }

 @Configuration
 @EnableAutoConfiguration(excludeName={"org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration", "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration"}, exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,JpaRepositoriesAutoConfiguration.class,FlywayAutoConfiguration.class})
 @Import({SecurityConfig.class, GatewaySecretFilter.class, Probe.class, PaymentController.class})
 static class Config { }
 @RestController static class Probe {
  @GetMapping("/api/v1/payments/probe") String user(@AuthenticationPrincipal AuthenticatedUser u, HttpServletRequest r) {
   return u.id()+":"+u.authorities()+":"+r.getHeader("X-User-Id")+":"+r.getHeader("X-User-Roles")+":"+r.getHeader("X-User-Email")+":"+r.getHeader("X-User-Cinema-Id")+":"+r.getHeader("X-Internal-Service-Secret");
  }
  @GetMapping("/internal/probe") String internal(){ return "ok"; }
 }
}
