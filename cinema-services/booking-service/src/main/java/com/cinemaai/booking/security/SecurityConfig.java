package com.cinemaai.booking.security;

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
import org.slf4j.MDC;
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
            @Value("${app.gateway.secret}") String gatewaySecret,
            @Value("${app.internal.secret}") String internalSecret,
            @Value("${app.jwt.secret}") String jwtSecret
    ) throws Exception {

        var key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        var filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {

                String correlation = request.getHeader("X-Correlation-Id");
                if (correlation == null || !correlation.matches("[a-zA-Z0-9._-]{1,100}")) {
                    correlation = UUID.randomUUID().toString();
                }
                MDC.put("correlationId", correlation);
                response.setHeader("X-Correlation-Id", correlation);

                try {
                    String path = request.getRequestURI();

                    // Whitelisted endpoints
                    if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || path.startsWith("/actuator/health") || path.startsWith("/v3/api-docs") || path.startsWith("/error")) {
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
                        writeError(mapper, request, response, 403, "Trusted service access required");
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
                            log.warn("JWT parse failed: {}", ex.getMessage());
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) -> writeError(mapper, req, res, 401, "Authentication required"))
                        .accessDeniedHandler((req, res, ex) -> writeError(mapper, req, res, 403, "Access denied")))
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

    private void writeError(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        com.cinemaai.booking.dto.response.ErrorResponse error = com.cinemaai.booking.dto.response.ErrorResponse.of(message, request.getRequestURI());
        mapper.writeValue(response.getWriter(), error);
    }
}
