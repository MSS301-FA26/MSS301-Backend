package com.cinemaai.payment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper mapper,
            @Value("${app.gateway.secret:8F78D52690EED1A48867F89272F07391B8FBC8968F187BB5C53C60E20243D7AD}") String gatewaySecret,
            @Value("${app.internal.secret:CF419427F61EE9D8880297E0309BDFB4504B8917C10B7019A56B1562344DDA03}") String internalSecret,
            @Value("${app.jwt.secret:cineai-development-secret-key-please-change-123456}") String jwtSecret
    ) throws Exception {

        var key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        var filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {

                String path = request.getRequestURI();

                // Whitelisted endpoints
                if (path.startsWith("/actuator/health") || path.startsWith("/v3/api-docs") || path.startsWith("/error")) {
                    chain.doFilter(request, response);
                    return;
                }

                // Check Gateway Secret or Internal Secret
                boolean internal = path.startsWith("/internal/");
                String suppliedSecret = request.getHeader(internal ? "X-Internal-Service-Secret" : "X-Gateway-Secret");
                String expectedSecret = internal ? internalSecret : gatewaySecret;

                if (suppliedSecret == null || !MessageDigest.isEqual(
                        expectedSecret.getBytes(StandardCharsets.UTF_8),
                        suppliedSecret.getBytes(StandardCharsets.UTF_8))) {
                    writeError(mapper, response, 403, "DIRECT_ACCESS_FORBIDDEN",
                            "Direct access to microservice is blocked. Requests must be routed through API Gateway (Port 8080).");
                    return;
                }

                // Authenticate from Header (Gateway Forwarding) or Bearer JWT Token
                String userIdHeader = request.getHeader("X-User-Id");
                String userRolesHeader = request.getHeader("X-User-Roles");
                String authHeader = request.getHeader("Authorization");

                if (userIdHeader != null && !userIdHeader.isBlank()) {
                    try {
                        Long uid = Long.parseLong(userIdHeader);
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        if (userRolesHeader != null && !userRolesHeader.isBlank()) {
                            for (String role : userRolesHeader.split(",")) {
                                String r = role.trim();
                                if (!r.startsWith("ROLE_")) r = "ROLE_" + r;
                                authorities.add(new SimpleGrantedAuthority(r));
                            }
                        }
                        AuthenticatedUser user = new AuthenticatedUser(uid, request.getHeader("X-User-Email"), authorities);
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, authorities));
                    } catch (Exception ex) {
                        log.warn("Invalid X-User-Id header: {}", userIdHeader);
                    }
                } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
                        Long uid = null;
                        Object uidObj = claims.get("userId");
                        if (uidObj instanceof Number num) {
                            uid = num.longValue();
                        } else if (claims.getSubject() != null) {
                            try {
                                uid = Long.parseLong(claims.getSubject());
                            } catch (Exception ignored) {}
                        }

                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        Object rawRoles = claims.get("roles");
                        if (rawRoles instanceof List<?> list) {
                            for (Object item : list) {
                                String r = String.valueOf(item);
                                if (!r.startsWith("ROLE_")) r = "ROLE_" + r;
                                authorities.add(new SimpleGrantedAuthority(r));
                            }
                        }

                        AuthenticatedUser user = new AuthenticatedUser(uid, claims.get("email", String.class), authorities);
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, authorities));
                    } catch (Exception ex) {
                        log.warn("JWT parse failed in payment-service: {}", ex.getMessage());
                    }
                }

                chain.doFilter(request, response);
            }
        };

        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                        .requestMatchers("/api/v1/payments/vnpay/ipn", "/api/v1/payments/vnpay/return").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) -> writeError(mapper, res, 401, "UNAUTHORIZED", "Authentication required"))
                        .accessDeniedHandler((req, res, ex) -> writeError(mapper, res, 403, "FORBIDDEN", "Access denied")))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeError(ObjectMapper mapper, HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = Map.of(
                "success", false,
                "code", code,
                "message", message
        );
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
