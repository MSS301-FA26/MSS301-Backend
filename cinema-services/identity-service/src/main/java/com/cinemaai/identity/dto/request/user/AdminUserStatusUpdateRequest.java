package com.cinemaai.identity.dto.request.user;

import com.cinemaai.identity.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUserStatusUpdateRequest(
        @NotNull(message = "Status is required")
        UserStatus status
) {
}
