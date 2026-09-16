package com.cinemaai.identity.dto.response.user;

import com.cinemaai.identity.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        Integer birthYear,
        UserStatus status,
        boolean emailVerified,
        boolean phoneVerified,
        List<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
