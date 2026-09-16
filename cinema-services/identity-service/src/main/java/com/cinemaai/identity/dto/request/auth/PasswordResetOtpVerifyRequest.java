package com.cinemaai.identity.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetOtpVerifyRequest(
        @Email(message = "Email is invalid")
        String email,

        @NotBlank(message = "OTP is required")
        String otp
) {
}
