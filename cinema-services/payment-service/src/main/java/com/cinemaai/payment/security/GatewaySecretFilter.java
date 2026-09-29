package com.cinemaai.payment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces Zero-Trust architecture: Blocks direct access to payment-service (port 8084).
 * Only requests routed through API Gateway (port 8080) with a valid X-Gateway-Secret header,
 * or internal requests from other microservices with a valid X-Internal-Service-Secret header, are accepted.
 */
@Component
public class GatewaySecretFilter extends OncePerRequestFilter {

    private final String gatewaySecret;
    private final String internalSecret;

    public GatewaySecretFilter(
            @Value("${app.gateway.secret}") String gatewaySecret,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.gatewaySecret = gatewaySecret;
        this.internalSecret = internalSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        // Whitelisted endpoints: Docker/K8s health check, VNPay callbacks, Swagger & preflight OPTIONS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/actuator/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/error")
                || path.equals("/api/v1/payments/vnpay/return")
                || path.equals("/api/v1/payments/vnpay/null")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isInternal = path.startsWith("/internal/");
        String headerName = isInternal ? "X-Internal-Service-Secret" : "X-Gateway-Secret";
        String expectedSecret = isInternal ? internalSecret : gatewaySecret;
        String supplied = request.getHeader(headerName);

        if (supplied == null || !MessageDigest.isEqual(
                expectedSecret.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                {
                    "success": false,
                    "code": "DIRECT_ACCESS_FORBIDDEN",
                    "message": "Direct access to microservice is blocked. Requests must be routed through API Gateway (Port 8080)."
                }
                """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
