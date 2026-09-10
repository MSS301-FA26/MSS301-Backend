package com.sba301.cinemaai.dto.request.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserProfileUpdateRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone is invalid")
        String phone,

        @Min(value = 1900, message = "Birth year must be after 1900")
        @Max(value = 2100, message = "Birth year is invalid")
        Integer birthYear
) {
}
