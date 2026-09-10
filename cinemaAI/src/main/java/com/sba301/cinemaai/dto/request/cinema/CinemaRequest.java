package com.sba301.cinemaai.dto.request.cinema;

import com.sba301.cinemaai.enums.CinemaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CinemaRequest(
        @NotBlank(message = "Cinema name is required")
        String name,

        @NotBlank(message = "Address is required")
        @Size(max = 500, message = "Address must be at most 500 characters")
        String address,

        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone is invalid")
        String phone,

        CinemaStatus status
) {
}
