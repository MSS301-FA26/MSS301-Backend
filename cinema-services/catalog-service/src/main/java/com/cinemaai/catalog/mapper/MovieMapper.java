package com.cinemaai.catalog.mapper;

import com.cinemaai.catalog.dto.response.movie.ActorResponse;
import com.cinemaai.catalog.dto.response.movie.GenreResponse;
import com.cinemaai.catalog.dto.response.movie.MovieResponse;
import com.cinemaai.catalog.entity.Actor;
import com.cinemaai.catalog.entity.Genre;
import com.cinemaai.catalog.entity.Movie;
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
                movie.getAgeRating() == null ? null : movie.getAgeRating().getLabel(),
                movie.getDirector(),
                movie.getMainActors(),
                movie.getCastList(),
                genres.stream().map(this::toGenreResponse).toList(),
                actors,
                mainActorIds,
                movie.getCreatedAt(),
                movie.getUpdatedAt()
        );
    }
}
