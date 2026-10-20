package com.cinemaai.catalog.dto.response.movie;

import com.cinemaai.catalog.enums.MovieStatus;
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
        LocalDateTime updatedAt,
        List<DirectorResponse> directors
) {
    public MovieResponse(
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
        this(id, title, description, trailerUrl, posterUrl, avatarUrl, durationMinutes,
             releaseDate, endDate, language, subtitleLanguage, status, ageRating, director,
             mainActors, castList, genres, actors, mainActorIds, createdAt, updatedAt, List.of());
    }
}
