package com.cinemaai.payment.client;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdentityScopeClientTest {
    @Test void scopeUsesInternalSecretAndRejectsErrorOrMalformedResponses() throws Exception {
        var body=new AtomicReference<>("{\"success\":true,\"data\":{\"userId\":7,\"email\":\"test\",\"status\":\"ACTIVE\",\"roles\":[\"MANAGER\"],\"cinemaId\":101}}");
        var status=new java.util.concurrent.atomic.AtomicInteger(200);
        var secret=new AtomicReference<String>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/internal/v1/users/7/scope",exchange->{
            secret.set(exchange.getRequestHeaders().getFirst("X-Internal-Service-Secret"));
            byte[] bytes=body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(status.get(),bytes.length);
            try(var output=exchange.getResponseBody()){output.write(bytes);}
        }); server.start();
        try {
            var client=new IdentityClient("http://127.0.0.1:"+server.getAddress().getPort(),"test-internal");
            assertEquals(101L,client.getUserAccessScope(7L).cinemaId()); assertEquals("test-internal",secret.get());
            for(int code:new int[]{403,500}) { status.set(code); assertNull(client.getUserAccessScope(7L)); }
            status.set(200);
            for(String invalid:new String[]{"invalid-json","null","{\"success\":false,\"data\":{\"userId\":7}}","{\"success\":true,\"data\":null}"}) {
                body.set(invalid); assertNull(client.getUserAccessScope(7L));
            }
        } finally { server.stop(0); }
    }
}
