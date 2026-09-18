package com.cinemaai.gateway;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TrustedGatewayFilter implements GlobalFilter, Ordered {

    private final String secret;

    public TrustedGatewayFilter(@Value("${INTERNAL_GATEWAY_SECRET:8F78D52690EED1A48867F89272F07391B8FBC8968F187BB5C53C60E20243D7AD}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Gateway secret is required");
        }
        this.secret = secret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Block external calls to internal routes
        if (exchange.getRequest().getURI().getPath().startsWith("/internal/")) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        String provided = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        String correlation = provided != null && provided.matches("[a-zA-Z0-9._-]{1,100}") ? provided : UUID.randomUUID().toString();

        var request = exchange.getRequest().mutate().headers(headers -> {
            headers.set("X-Gateway-Secret", secret);
            headers.remove("X-Internal-Service-Secret");
            headers.set("X-Correlation-Id", correlation);
        }).build();

        exchange.getResponse().getHeaders().set("X-Correlation-Id", correlation);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
