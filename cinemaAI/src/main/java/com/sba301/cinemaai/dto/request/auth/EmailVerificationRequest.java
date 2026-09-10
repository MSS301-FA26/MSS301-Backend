package com.sba301.cinemaai.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationRequest(
        String email,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP is invalid")
        String otp
) {
    public EmailVerificationRequest(String token) {
        this(null, token);
    }
}
