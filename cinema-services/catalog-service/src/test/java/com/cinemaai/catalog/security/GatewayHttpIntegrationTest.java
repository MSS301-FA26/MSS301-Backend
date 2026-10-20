package com.cinemaai.catalog.security;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes=GatewayHttpIntegrationTest.Config.class, properties={"spring.servlet.multipart.max-file-size=1MB", "spring.servlet.multipart.max-request-size=1MB", "APPLICATION_NAME=http-test", "management.endpoints.web.exposure.include=health,info", "management.endpoint.health.show-details=never", "springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false", "server.address=127.0.0.1", "app.gateway.secret=gateway-test", "app.internal.secret=internal-test", "app.jwt.secret=01234567890123456789012345678901", "LOG_LEVEL_PATTERN=%5p"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GatewayHttpIntegrationTest {
 @LocalServerPort int downstreamPort; GatewayHttpHarness gateway;
 @BeforeAll void start() throws Exception {gateway=new GatewayHttpHarness(downstreamPort);}
 @AfterAll void stop() throws Exception {if(gateway!=null)gateway.close();}
 String token(String role,String key){return Jwts.builder().subject("a@example.test").claim("userId",7L).claim("roles",List.of(role)).expiration(new Date(System.currentTimeMillis()+60000)).signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();}
 @Test void publicAndAdminThroughGateway() throws Exception {
  assertEquals(200,gateway.send("/api/v1/movies/probe",Map.of()).statusCode());
  assertEquals(401,gateway.send("/api/v1/admin/system-settings/probe",Map.of()).statusCode());
  assertEquals(401,gateway.send("/api/v1/admin/system-settings/probe",Map.of("Authorization","Bearer "+token("ADMIN","abcdefghijklmnopqrstuvwxyz123456"))).statusCode());
  var forged=Map.of("Authorization","Bearer "+token("USER","01234567890123456789012345678901"),"X-User-Id","9","X-User-Roles","ADMIN");
  assertEquals(403,gateway.send("/api/v1/admin/system-settings/probe",forged).statusCode());
  var admin=gateway.send("/api/v1/admin/system-settings/probe",Map.of("Authorization","Bearer "+token("ADMIN","01234567890123456789012345678901"),"X-User-Id","9","X-User-Roles","USER"));
  assertEquals(200,admin.statusCode()); assertEquals("7:[ROLE_ADMIN]:null:null",admin.body());
 }
 @Test void gatewaySecretDefinesDirectTrustBoundary() throws Exception {
  assertEquals(403,gateway.sendTo(downstreamPort,"/api/v1/admin/system-settings/probe",Map.of("X-User-Id","9","X-User-Roles","ADMIN")).statusCode());
  var direct=gateway.sendTo(downstreamPort,"/api/v1/admin/system-settings/probe",Map.of("X-Gateway-Secret","gateway-test","X-User-Id","9","X-User-Roles","ADMIN"));
  assertEquals(200,direct.statusCode()); assertEquals("9:[ROLE_ADMIN]:9:ADMIN",direct.body());
 }
 @Configuration @EnableAutoConfiguration(excludeName={"org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration", "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration"}, exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,JpaRepositoriesAutoConfiguration.class,FlywayAutoConfiguration.class})
 @Import({SecurityConfig.class,Probe.class}) static class Config { }
 @RestController static class Probe {
  @GetMapping("/api/v1/movies/probe") String pub(){return "ok";}
  @GetMapping("/api/v1/admin/system-settings/probe") String admin(@AuthenticationPrincipal AuthenticatedUser u,HttpServletRequest r){return u.id()+":"+u.authorities()+":"+r.getHeader("X-User-Id")+":"+r.getHeader("X-User-Roles");}
 }
}
