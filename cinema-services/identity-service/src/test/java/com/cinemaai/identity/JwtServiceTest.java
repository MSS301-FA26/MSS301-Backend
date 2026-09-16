package com.cinemaai.identity;

import com.cinemaai.identity.config.JwtProperties;
import com.cinemaai.identity.security.JwtService;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(SECRET, 900000L, 604800000L);
        jwtService = new JwtService(properties);
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        Long userId = 42L;
        String email = "test@cinemaai.com";
        List<String> roles = List.of("CUSTOMER", "ADMIN");

        String token = jwtService.generateAccessToken(userId, email, roles);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));

        assertEquals(email, jwtService.getSubject(token));
        assertEquals(userId, jwtService.getUserId(token));
        assertEquals(roles, jwtService.getRoles(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.here"));
    }
}

