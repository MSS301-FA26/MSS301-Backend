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

import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper,
            @Value("${app.gateway.secret}") String gatewaySecret,
            @Value("${app.internal.secret}") String internalSecret,
            @Value("${app.jwt.secret}") String jwtSecret) throws Exception {
        if (gatewaySecret == null || gatewaySecret.isBlank() || internalSecret == null || internalSecret.isBlank()) {
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
                    if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                            || path.startsWith("/actuator/health")
                            || path.startsWith("/v3/api-docs")
                            || path.startsWith("/error")) {
                        chain.doFilter(request, response);
                        return;
                    }
                    String supplied = request.getHeader(internal ? "X-Internal-Service-Secret" : "X-Gateway-Secret");
                    String expected = internal ? internalSecret : gatewaySecret;
                    if (supplied == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
                        writeError(mapper, request, response, 403, "Trusted service access required");
                        return;
                    }
                    // Check headers from trusted Gateway or Bearer JWT Token
                    String userIdHeader = request.getHeader("X-User-Id");
                    String userRolesHeader = request.getHeader("X-User-Roles");
                    String userCinemaHeader = request.getHeader("X-User-Cinema-Id");
                    String bearer = request.getHeader("Authorization");

                    if (userIdHeader != null && !userIdHeader.isBlank()) {
                        try {
                            Long uid = Long.parseLong(userIdHeader.trim());
                            Long cinemaId = null;
                            if (userCinemaHeader != null && !userCinemaHeader.isBlank()) {
                                try {
                                    cinemaId = Long.parseLong(userCinemaHeader.trim());
                                } catch (Exception ignored) {}
                            }
                            java.util.List<SimpleGrantedAuthority> roles = new java.util.ArrayList<>();
                            if (userRolesHeader != null && !userRolesHeader.isBlank()) {
                                for (String role : userRolesHeader.split(",")) {
                                    String r = role.trim();
                                    if (!r.startsWith("ROLE_")) r = "ROLE_" + r;
                                    roles.add(new SimpleGrantedAuthority(r));
                                }
                            }
                            AuthenticatedUser user = new AuthenticatedUser(uid, request.getHeader("X-User-Email"), roles, cinemaId);
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(user, null, roles));
                        } catch (Exception ignored) {}
                    } else if (bearer != null && bearer.startsWith("Bearer ") && !internal) {
                        try {
                            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(bearer.substring(7)).getPayload();
                            Long uid = null;
                            Object uidObj = claims.get("userId");
                            if (uidObj instanceof Number num) {
                                uid = num.longValue();
                            } else if (claims.getSubject() != null) {
                                try {
                                    uid = Long.parseLong(claims.getSubject());
                                } catch (Exception ignored) {}
                            }

                            Long cinemaId = null;
                            Object cinemaObj = claims.get("cinemaId");
                            if (cinemaObj instanceof Number cNum) {
                                cinemaId = cNum.longValue();
                            } else if (cinemaObj instanceof String cStr && !cStr.isBlank()) {
                                try {
                                    cinemaId = Long.parseLong(cStr.trim());
                                } catch (Exception ignored) {}
                            }

                            Object rawRoles = claims.get("roles");
                            List<SimpleGrantedAuthority> roles = rawRoles instanceof List<?> values ? values.stream()
                                    .filter(String.class::isInstance).map(String.class::cast)
                                    .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role)).toList() : List.of();

                            String email = claims.get("email", String.class);
                            if (email == null) {
                                email = claims.getSubject();
                            }

                            AuthenticatedUser user = new AuthenticatedUser(uid, email, roles, cinemaId);
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(user, null, roles));
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/cinemas", "/api/v1/admin/cinemas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admin/cinemas/**", "/api/v1/admin/cinema").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/cinemas/**", "/api/v1/admin/cinema/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/cinemas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/movies", "/api/v1/admin/movies/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admin/movies/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/movies/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/movies/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/genres", "/api/v1/admin/genres/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admin/genres/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/genres/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/actors", "/api/v1/admin/actors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admin/actors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/actors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/directors", "/api/v1/admin/directors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admin/directors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/directors/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/system-settings", "/api/v1/admin/system-settings/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/ticket-pricing/validate", "/api/v1/catalog/checkout-quote").permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> writeError(mapper, request, response, 401, "Authentication required"))
                        .accessDeniedHandler((request, response, exception) -> writeError(mapper, request, response, 403, "Access denied")))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class).build();
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

    private static void writeError(ObjectMapper mapper, HttpServletRequest request,
            HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getWriter(), ErrorResponse.of(message, request.getRequestURI()));
    }
}
