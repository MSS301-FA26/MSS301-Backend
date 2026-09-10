package com.sba301.cinemaai.dto.response.movie;

import com.sba301.cinemaai.enums.MovieStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MovieResponse(
        Long id,
        String title,
        String description,
        String trailerUrl,
        String posterUrl,
        String avatarUrl,
        int durationMinutes,
        LocalDate releaseDate,
        LocalDate endDate,
        String language,
        String subtitleLanguage,
        MovieStatus status,
        String ageRating,
        String director,
        String mainActors,
        String castList,
        List<GenreResponse> genres,
        List<ActorResponse> actors,
        List<Long> mainActorIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
