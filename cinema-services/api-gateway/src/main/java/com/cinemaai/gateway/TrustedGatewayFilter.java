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
    private final String aiSecret;

    public TrustedGatewayFilter(
            @Value("${INTERNAL_GATEWAY_SECRET}") String secret,
            @Value("${AI_GATEWAY_SECRET:${INTERNAL_GATEWAY_SECRET}}") String aiSecret
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Gateway secret is required");
        }
        this.secret = secret;
        this.aiSecret = aiSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getURI().getPath().startsWith("/internal/")) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        String provided = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        String correlation = provided != null && provided.matches("[a-zA-Z0-9._-]{1,100}") ? provided : UUID.randomUUID().toString();

        String path = exchange.getRequest().getURI().getPath();
        String effectiveSecret = (path.startsWith("/api/v1/recommendation") || path.startsWith("/api/v1/chat") || path.startsWith("/api/v1/search"))
                ? aiSecret
                : secret;

        var request = exchange.getRequest().mutate().headers(headers -> {
            headers.set("X-Gateway-Secret", effectiveSecret);
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
