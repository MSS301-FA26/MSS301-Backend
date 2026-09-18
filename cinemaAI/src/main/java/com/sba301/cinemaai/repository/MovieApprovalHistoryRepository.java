package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.MovieApprovalHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieApprovalHistoryRepository extends JpaRepository<MovieApprovalHistory, Long> {

    @Query("SELECT h FROM MovieApprovalHistory h LEFT JOIN FETCH h.actor WHERE h.movie.id = :movieId ORDER BY h.createdAt DESC")
    List<MovieApprovalHistory> findWithActorByMovieIdOrderByCreatedAtDesc(@Param("movieId") Long movieId);

    List<MovieApprovalHistory> findByMovieOrderByCreatedAtDesc(Movie movie);

    void deleteByMovie(Movie movie);
}
