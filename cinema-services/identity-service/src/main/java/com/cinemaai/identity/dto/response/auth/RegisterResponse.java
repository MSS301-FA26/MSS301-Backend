package com.cinemaai.identity.dto.response.auth;

import com.cinemaai.identity.dto.response.user.UserProfileResponse;

public record RegisterResponse(
        UserProfileResponse user,
        boolean emailVerificationRequired,
        long emailVerificationExpiresInSeconds
) {
}
