package com.sba301.cinemaai.dto.request.movie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenreRequest(
        @NotBlank(message = "Genre name is required")
        @Size(max = 100, message = "Genre name must be at most 100 characters")
        String name,

        @NotBlank(message = "Description is required")
        @Size(min = 50, max = 1000, message = "Description must be between 50 and 1000 characters")
        String description
) {
}
