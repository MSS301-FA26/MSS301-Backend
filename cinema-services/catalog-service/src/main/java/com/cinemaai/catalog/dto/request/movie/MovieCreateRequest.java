package com.cinemaai.catalog.dto.request.movie;

import com.cinemaai.catalog.enums.MovieStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record MovieCreateRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotBlank(message = "Trailer URL is required")
        @Size(max = 500, message = "Trailer URL must be at most 500 characters")
        String trailerUrl,

        @NotBlank(message = "Poster URL is required")
        @Size(max = 500, message = "Poster URL must be at most 500 characters")
        String posterUrl,

        @Size(max = 500, message = "Avatar URL must be at most 500 characters")
        String avatarUrl,

        @Min(value = 1, message = "Duration must be positive")
        int durationMinutes,

        LocalDate releaseDate,
        LocalDate endDate,
        String language,
        String subtitleLanguage,

        MovieStatus status,

        String ageRating,
        String director,
        List<Long> genreIds,

        List<Long> actorIds,

        List<Long> mainActorIds
) {
}
