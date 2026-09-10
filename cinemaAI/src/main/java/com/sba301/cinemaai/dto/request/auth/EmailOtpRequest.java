package com.sba301.cinemaai.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailOtpRequest(
        @Email(message = "Email is invalid")
        @NotBlank(message = "Email is required")
        String email
) {
}
