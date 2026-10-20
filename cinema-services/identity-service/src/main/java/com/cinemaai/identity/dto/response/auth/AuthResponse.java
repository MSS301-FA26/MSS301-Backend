package com.cinemaai.identity.dto.response.auth;

import com.cinemaai.identity.dto.response.user.UserProfileResponse;
import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UserProfileResponse user,
        List<String> roles
) {
}
