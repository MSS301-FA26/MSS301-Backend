package com.sba301.cinemaai.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "Email is invalid")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Password must contain uppercase, lowercase, number, and special character"
        )
        String password,

        @NotBlank(message = "Full name is required")
        String fullName,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone is invalid")
        String phone,

        @Min(value = 1900, message = "Birth year must be after 1900")
        @Max(value = 2100, message = "Birth year is invalid")
        Integer birthYear
) {
}
