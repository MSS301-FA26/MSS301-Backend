package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.Actor;
import com.cinemaai.catalog.entity.Movie;
import com.cinemaai.catalog.entity.MovieActor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieActorRepository extends JpaRepository<MovieActor, Long> {

    List<MovieActor> findByMovie(Movie movie);

    List<MovieActor> findByActor(Actor actor);

    long countByActor(Actor actor);

    List<MovieActor> findByMovieId(Long movieId);

    @Query("""
            select movieActor
            from MovieActor movieActor
            join fetch movieActor.actor
            where movieActor.movie.id in :movieIds
            order by movieActor.movie.id asc, movieActor.mainActor desc, movieActor.actor.name asc
            """)
    List<MovieActor> findWithActorByMovieIdIn(@Param("movieIds") List<Long> movieIds);

    boolean existsByMovieAndActor(Movie movie, Actor actor);

    void deleteByMovie(Movie movie);
}
