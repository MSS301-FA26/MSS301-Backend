package com.sba301.cinemaai.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationRequest(
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone is invalid")
        String phone,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP is invalid")
        String otp
) {
}
