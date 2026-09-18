package com.cinemaai.catalog.security;

import com.cinemaai.catalog.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper,
            @Value("${app.gateway.secret}") String gatewaySecret,
            @Value("${app.internal.secret}") String internalSecret,
            @Value("${app.jwt.secret}") String jwtSecret) throws Exception {
        if (gatewaySecret.isBlank() || internalSecret.isBlank()) {
            throw new IllegalArgumentException("Gateway and internal service secrets must not be blank");
        }
        var key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        var filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain chain) throws ServletException, IOException {
                String correlation = request.getHeader("X-Correlation-Id");
                if (correlation == null || !correlation.matches("[a-zA-Z0-9._-]{1,100}")) correlation = UUID.randomUUID().toString();
                MDC.put("correlationId", correlation);
                response.setHeader("X-Correlation-Id", correlation);
                try {
                    String path = request.getRequestURI();
                    boolean internal = path.startsWith("/internal/");
                    if (!path.equals("/actuator/health")) {
                        String supplied = request.getHeader(internal ? "X-Internal-Service-Secret" : "X-Gateway-Secret");
                        String expected = internal ? internalSecret : gatewaySecret;
                        if (supplied == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
                            writeError(mapper, request, response, 403, "Trusted service access required");
                            return;
                        }
                    }
                    String bearer = request.getHeader("Authorization");
                    if (bearer != null && bearer.startsWith("Bearer ") && !internal) {
                        try {
                            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(bearer.substring(7)).getPayload();
                            Object rawRoles = claims.get("roles");
                            List<SimpleGrantedAuthority> roles = rawRoles instanceof List<?> values ? values.stream()
                                    .filter(String.class::isInstance).map(String.class::cast)
                                    .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role)).toList() : List.of();
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, roles));
                        } catch (RuntimeException exception) {
                            writeError(mapper, request, response, 401, "Invalid or expired access token");
                            return;
                        }
                    }
                    chain.doFilter(request, response);
                } finally {
                    MDC.remove("correlationId");
                }
            }
        };
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/internal/v1/catalog/checkout-quote").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/ticket-pricing/validate", "/api/v1/catalog/checkout-quote").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> writeError(mapper, request, response, 401, "Authentication required"))
                        .accessDeniedHandler((request, response, exception) -> writeError(mapper, request, response, 403, "Access denied")))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class).build();
    }

    private static void writeError(ObjectMapper mapper, HttpServletRequest request,
            HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), ErrorResponse.of(message, request.getRequestURI()));
    }
}
