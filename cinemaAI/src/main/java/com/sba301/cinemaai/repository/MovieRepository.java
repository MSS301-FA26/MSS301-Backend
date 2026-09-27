package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.enums.MovieStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    Optional<Movie> findByTitle(String title);
    Optional<Movie> findByTitleIgnoreCase(String title);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select movie from Movie movie where movie.id = :id")
    Optional<Movie> findForEditRequest(@Param("id") Long id);

    boolean existsByTitle(String title);
    boolean existsByTitleIgnoreCase(String title);

    List<Movie> findByStatus(MovieStatus status);

    List<Movie> findByReleaseDateBetween(LocalDate from, LocalDate to);
}
