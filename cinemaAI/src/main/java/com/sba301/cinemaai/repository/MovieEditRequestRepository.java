package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.MovieEditRequest;
import com.sba301.cinemaai.enums.MovieEditRequestStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieEditRequestRepository extends JpaRepository<MovieEditRequest, Long> {
    @EntityGraph(attributePaths = {"movie", "requestedBy"})
    List<MovieEditRequest> findByRequestedByIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"movie", "requestedBy"})
    List<MovieEditRequest> findByStatusOrderByCreatedAtAsc(MovieEditRequestStatus status);

    @EntityGraph(attributePaths = {"movie", "requestedBy"})
    List<MovieEditRequest> findAllByOrderByCreatedAtDesc();

    boolean existsByMovieIdAndStatus(Long movieId, MovieEditRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from MovieEditRequest request where request.id = :id")
    Optional<MovieEditRequest> findForReview(@Param("id") Long id);
}

