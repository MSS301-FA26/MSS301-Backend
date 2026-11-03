package com.cinemaai.identity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import javax.sql.DataSource;
import java.nio.file.*;
import java.util.*;
import java.net.*;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"LOG_LEVEL_PATTERN=%5p", "logging.pattern.level=%5p",
        "spring.config.import=file:.env.example[.properties]", "spring.application.name=identity-service-local-verify"})
@EnabledIfSystemProperty(named="local.db.verify",matches="true")
class LocalDatabaseRuntimeVerificationTest {
    @DynamicPropertySource static void configuration(DynamicPropertyRegistry registry) throws Exception {
        Map<String,String> values=new HashMap<>();
        for(String line:Files.readAllLines(Path.of(".env.example"))) {
            if(!line.isBlank() && !line.stripLeading().startsWith("#") && line.contains("=")) {
                int i=line.indexOf('='); values.put(line.substring(0,i).trim(),line.substring(i+1).trim());
            }
        }
        String url=System.getProperty("local.db.identity.url");
        if(url==null || !url.equals("jdbc:postgresql://127.0.0.1:57101/identity_db"))
            throw new IllegalArgumentException("Explicit task052 disposable datasource required");
        values.put("spring.config.import",""); // Do not import any developer .env.
        values.put("spring.datasource.url",url);
        values.put("spring.datasource.username","identity_verify");
        values.put("spring.datasource.password",System.getProperty("local.db.password"));
        values.put("spring.datasource.driver-class-name","org.postgresql.Driver");
        values.put("spring.jpa.hibernate.ddl-auto","validate");
        values.put("spring.flyway.enabled","true");
        values.put("server.address","127.0.0.1");
        values.put("server.port","0");
        values.put("spring.rabbitmq.host","127.0.0.1");
        values.put("spring.rabbitmq.port","57172");
        values.put("spring.rabbitmq.username","local_verify");
        values.put("spring.rabbitmq.password",System.getProperty("local.db.password"));
        values.put("app.mail.enabled","false");
        values.put("management.health.mail.enabled","false");
        values.put("catalog.service.url","http://127.0.0.1:15802");
        values.put("identity.service.url","http://127.0.0.1:15801");
        values.put("booking.service.url","http://127.0.0.1:15803");
        values.put("payment.service.url","http://127.0.0.1:15804");
        values.put("app.gateway.url","http://127.0.0.1:15800");
        values.put("app.gateway.secret","task052-gateway-test");
        values.put("app.internal.secret","task052-internal-test");
        values.put("app.jwt.secret","task052-test-only-jwt-key-01234567890123456789");
        values.put("booking.hold.cleanup.enabled","false");
        values.put("food-order.payment.cleanup.enabled","false");
        values.forEach((k,v)->registry.add(k,()->v));
    }
    @Autowired DataSource datasource;
    @LocalServerPort int port;
    @Test void effectiveDatasourceMigrationAndHttpHealthMatchOwnedDatabase() throws Exception {
        try(var connection=datasource.getConnection();var statement=connection.createStatement()) {
            assertEquals("jdbc:postgresql://127.0.0.1:57101/identity_db",connection.getMetaData().getURL());
            try(var result=statement.executeQuery("select current_database(),current_user,inet_server_addr()::text,inet_server_port()")) {
                assertTrue(result.next());
                assertEquals("identity_db",result.getString(1)); assertEquals("identity_verify",result.getString(2));
                System.out.println("LOCAL_DB_EVIDENCE service=identity-service database="+result.getString(1)+" user="+result.getString(2)+" server="+result.getString(3)+":"+result.getInt(4)+" hostPort=57101 appPort="+port);
            }
            try(var result=statement.executeQuery("select count(*) from flyway_schema_history where success")) {
                assertTrue(result.next()); assertTrue(result.getInt(1)>0);
                System.out.println("LOCAL_DB_MIGRATIONS service=identity-service successful="+result.getInt(1));
            }
        }
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/actuator/health")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode()); assertTrue(response.body().contains("\"UP\""));
        System.out.println("LOCAL_DB_HEALTH service=identity-service status="+response.statusCode());
    }
}
