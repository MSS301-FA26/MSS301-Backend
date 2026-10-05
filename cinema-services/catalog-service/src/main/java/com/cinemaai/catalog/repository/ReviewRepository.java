package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.Movie;
import com.cinemaai.catalog.entity.Review;
import com.cinemaai.catalog.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Optional<Review> findByUserIdAndMovieId(Long userId, Long movieId);

    @Query("SELECT r FROM Review r WHERE r.userId = :userId AND r.movie.id = :movieId AND r.status NOT IN :excludedStatuses")
    Optional<Review> findActiveByUserAndMovie(
            @Param("userId") Long userId,
            @Param("movieId") Long movieId,
            @Param("excludedStatuses") Collection<ReviewStatus> excludedStatuses
    );

    boolean existsByUserIdAndMovieIdAndStatusNotIn(Long userId, Long movieId, Collection<ReviewStatus> statuses);

    Page<Review> findByMovieIdAndStatus(Long movieId, ReviewStatus status, Pageable pageable);

    Page<Review> findByMovieIdAndStatusIn(Long movieId, Collection<ReviewStatus> statuses, Pageable pageable);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByMovieIdAndStatusIn(Long movieId, Collection<ReviewStatus> statuses);

    long countByMovieIdAndVerifiedBookingTrueAndStatusIn(Long movieId, Collection<ReviewStatus> statuses);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.movie.id = :movieId AND r.status IN :statuses")
    Double getAverageRatingByMovie(
            @Param("movieId") Long movieId,
            @Param("statuses") Collection<ReviewStatus> statuses
    );

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.movie.id = :movieId AND r.status IN :statuses GROUP BY r.rating")
    List<Object[]> getRatingCountsByMovie(
            @Param("movieId") Long movieId,
            @Param("statuses") Collection<ReviewStatus> statuses
    );

    // Global Admin KPI stats
    long countByStatus(ReviewStatus status);

    long countByStatusIn(Collection<ReviewStatus> statuses);

    long countByVerifiedBookingTrueAndStatusIn(Collection<ReviewStatus> statuses);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.status IN :statuses")
    Double getOverallAverageRating(@Param("statuses") Collection<ReviewStatus> statuses);
}
