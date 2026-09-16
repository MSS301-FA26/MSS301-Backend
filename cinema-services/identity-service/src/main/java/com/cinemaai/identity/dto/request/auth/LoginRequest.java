package com.cinemaai.identity.dto.request.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @JsonAlias("email")
        @NotBlank(message = "Username/Email is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}

