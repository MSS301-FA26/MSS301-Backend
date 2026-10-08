package com.cinemaai.identity.dto.response.user;

import com.cinemaai.identity.enums.UserStatus;
import java.util.List;

public record UserAccessScopeResponse(
        Long userId,
        String email,
        UserStatus status,
        List<String> roles,
        Long cinemaId
) {
}
