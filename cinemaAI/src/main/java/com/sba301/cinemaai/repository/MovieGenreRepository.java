package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Genre;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.MovieGenre;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieGenreRepository extends JpaRepository<MovieGenre, Long> {

    List<MovieGenre> findByMovie(Movie movie);

    List<MovieGenre> findByGenre(Genre genre);

    List<MovieGenre> findByMovieId(Long movieId);

    @Query("""
            select movieGenre
            from MovieGenre movieGenre
            join fetch movieGenre.genre
            where movieGenre.movie.id in :movieIds
            order by movieGenre.movie.id asc, movieGenre.genre.name asc
            """)
    List<MovieGenre> findWithGenreByMovieIdIn(@Param("movieIds") List<Long> movieIds);

    boolean existsByMovieAndGenre(Movie movie, Genre genre);

    @Modifying
    @Query("delete from MovieGenre movieGenre where movieGenre.movie = :movie")
    void deleteByMovie(@Param("movie") Movie movie);
}
