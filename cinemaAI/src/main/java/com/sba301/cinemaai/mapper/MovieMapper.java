package com.sba301.cinemaai.mapper;

import com.sba301.cinemaai.dto.response.movie.ActorResponse;
import com.sba301.cinemaai.dto.response.movie.GenreResponse;
import com.sba301.cinemaai.dto.response.movie.MovieResponse;
import com.sba301.cinemaai.entity.Actor;
import com.sba301.cinemaai.entity.Genre;
import com.sba301.cinemaai.entity.Movie;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MovieMapper {

    public GenreResponse toGenreResponse(Genre genre) {
        return new GenreResponse(
                genre.getId(),
                genre.getName(),
                genre.getDescription(),
                genre.getCreatedAt(),
                genre.getUpdatedAt()
        );
    }

    public ActorResponse toActorResponse(Actor actor, long movieCount) {
        return new ActorResponse(
                actor.getId(),
                actor.getName(),
                actor.getBiography(),
                actor.getAvatarUrl(),
                movieCount,
                actor.getCreatedAt(),
                actor.getUpdatedAt()
        );
    }

    public MovieResponse toMovieResponse(
            Movie movie,
            List<Genre> genres,
            List<ActorResponse> actors,
            List<Long> mainActorIds
    ) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getTrailerUrl(),
                movie.getPosterUrl(),
                movie.getAvatarUrl(),
                movie.getDurationMinutes(),
                movie.getReleaseDate(),
                movie.getEndDate(),
                movie.getLanguage(),
                movie.getSubtitleLanguage(),
                movie.getStatus(),
                movie.getApprovalStatus(),
                movie.getPublicationStatus(),
                movie.getAgeRating() == null ? null : movie.getAgeRating().getLabel(),
                movie.getDirector(),
                movie.getMainActors(),
                actors != null ? actors.stream().map(ActorResponse::name).collect(java.util.stream.Collectors.joining(", ")) : null,
                genres.stream().map(this::toGenreResponse).toList(),
                actors,
                mainActorIds,
                movie.getSubmittedAt(),
                movie.getSubmittedBy() == null ? null : movie.getSubmittedBy().getId(),
                resolveUserName(movie.getSubmittedBy()),
                movie.getApprovedAt(),
                movie.getApprovedBy() == null ? null : movie.getApprovedBy().getId(),
                resolveUserName(movie.getApprovedBy()),
                movie.getRejectedAt(),
                movie.getRejectedBy() == null ? null : movie.getRejectedBy().getId(),
                resolveUserName(movie.getRejectedBy()),
                movie.getRejectionReason(),
                movie.getPublishedAt(),
                movie.getCreatedAt(),
                movie.getUpdatedAt()
        );
    }

    private String resolveUserName(com.sba301.cinemaai.entity.User user) {
        if (user == null) return null;
        if (user.getProfile() != null && user.getProfile().getFullName() != null && !user.getProfile().getFullName().isBlank()) {
            return user.getProfile().getFullName();
        }
        return user.getEmail();
    }
}
