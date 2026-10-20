package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.MovieDirector;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieDirectorRepository extends JpaRepository<MovieDirector, Long> {

    List<MovieDirector> findByMovieId(Long movieId);

    List<MovieDirector> findByDirectorId(Long directorId);

    void deleteByMovieId(Long movieId);

    boolean existsByDirectorId(Long directorId);
}
