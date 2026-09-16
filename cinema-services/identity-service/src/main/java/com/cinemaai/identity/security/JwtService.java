package com.cinemaai.identity.security;

import com.cinemaai.identity.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String ROLES_CLAIM = "roles";
    private static final String USER_ID_CLAIM = "userId";
    private static final String EMAIL_CLAIM = "email";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String email, Collection<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim(USER_ID_CLAIM, userId)
                .claim(EMAIL_CLAIM, email)
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(properties.accessExpirationMs())))
                .signWith(signingKey)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Object userIdObj = parseClaims(token).get(USER_ID_CLAIM);
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public List<String> getRoles(String token) {
        Object roles = parseClaims(token).get(ROLES_CLAIM);
        if (roles instanceof List<?> roleList) {
            return roleList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
