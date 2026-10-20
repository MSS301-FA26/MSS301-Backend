package com.cinemaai.catalog.mapper;

import com.cinemaai.catalog.dto.response.movie.ActorResponse;
import com.cinemaai.catalog.dto.response.movie.DirectorResponse;
import com.cinemaai.catalog.dto.response.movie.GenreResponse;
import com.cinemaai.catalog.dto.response.movie.MovieResponse;
import com.cinemaai.catalog.entity.Actor;
import com.cinemaai.catalog.entity.Director;
import com.cinemaai.catalog.entity.Genre;
import com.cinemaai.catalog.entity.Movie;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class MovieMapper {

    public abstract GenreResponse toGenreResponse(Genre genre);

    public abstract DirectorResponse toDirectorResponse(Director director);

    @Mapping(target = "id", source = "actor.id")
    @Mapping(target = "name", source = "actor.name")
    @Mapping(target = "biography", source = "actor.biography")
    @Mapping(target = "avatarUrl", source = "actor.avatarUrl")
    @Mapping(target = "movieCount", source = "movieCount")
    @Mapping(target = "createdAt", source = "actor.createdAt")
    @Mapping(target = "updatedAt", source = "actor.updatedAt")
    public abstract ActorResponse toActorResponse(Actor actor, long movieCount);

    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "title", source = "movie.title")
    @Mapping(target = "description", source = "movie.description")
    @Mapping(target = "trailerUrl", source = "movie.trailerUrl")
    @Mapping(target = "posterUrl", source = "movie.posterUrl")
    @Mapping(target = "avatarUrl", source = "movie.avatarUrl")
    @Mapping(target = "durationMinutes", source = "movie.durationMinutes")
    @Mapping(target = "releaseDate", source = "movie.releaseDate")
    @Mapping(target = "endDate", source = "movie.endDate")
    @Mapping(target = "language", source = "movie.language")
    @Mapping(target = "subtitleLanguage", source = "movie.subtitleLanguage")
    @Mapping(target = "status", source = "movie.status")
    @Mapping(target = "ageRating", expression = "java(movie.getAgeRating() == null ? null : movie.getAgeRating().getLabel())")
    @Mapping(target = "director", source = "movie.director")
    @Mapping(target = "mainActors", source = "movie.mainActors")
    @Mapping(target = "castList", source = "movie.castList")
    @Mapping(target = "genres", expression = "java(genres != null ? genres.stream().map(this::toGenreResponse).toList() : java.util.List.of())")
    @Mapping(target = "actors", source = "actors")
    @Mapping(target = "mainActorIds", source = "mainActorIds")
    @Mapping(target = "createdAt", source = "movie.createdAt")
    @Mapping(target = "updatedAt", source = "movie.updatedAt")
    public abstract MovieResponse toMovieResponse(
            Movie movie,
            List<Genre> genres,
            List<ActorResponse> actors,
            List<Long> mainActorIds
    );
}
