package com.cinemaai.gateway;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AiGatewayHttpIntegrationTest {
 @Test void aiRoutesForwardBearerAndStripUntrustedHeaders() throws Exception {
  HttpServer upstream=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  upstream.createContext("/", exchange -> {
   var h=exchange.getRequestHeaders();
   String body=exchange.getRequestURI().getPath()+":"+h.getFirst("Authorization")+":"+h.getFirst("X-Gateway-Secret")+":"+h.getFirst("X-User-Id")+":"+h.getFirst("X-User-Roles")+":"+h.getFirst("X-Internal-Service-Secret");
   byte[] bytes=body.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
  });
  upstream.start();
  try (var gateway=new GatewayHttpHarness(upstream.getAddress().getPort())) {
   for (String path : new String[]{"/api/v1/chat", "/api/v1/recommendations"}) {
    var result=gateway.send(path,Map.of("Authorization","Bearer test-token","X-User-Id","9","X-User-Roles","ADMIN","X-Internal-Service-Secret","fake"));
    assertEquals(200,result.statusCode()); assertEquals(path+":Bearer test-token:ai-test:null:null:null",result.body());
   }
  } finally {upstream.stop(0);}
 }
}
