package com.cinemaai.identity.dto.response.user;

import com.cinemaai.identity.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String email,
        String username,
        String fullName,
        String phone,
        String avatarUrl,
        Integer birthYear,
        Long preferredCinemaId,
        UserStatus status,
        boolean emailVerified,
        boolean phoneVerified,
        List<String> roles,
        Long cinemaId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public UserProfileResponse(
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
        this(id, email, null, fullName, phone, avatarUrl, birthYear, null, status, emailVerified, phoneVerified, roles, null, createdAt, updatedAt);
    }
}
