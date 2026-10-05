package com.cinemaai.identity.dto.request.user;

import jakarta.validation.constraints.NotNull;

public record AssignCinemaRequest(
        @NotNull(message = "Cinema ID is required")
        Long cinemaId
) {
}
