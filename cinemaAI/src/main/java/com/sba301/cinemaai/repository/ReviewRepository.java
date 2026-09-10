package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.Review;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.ReviewStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByMovieAndStatus(Movie movie, ReviewStatus status);

    Page<Review> findByMovieAndStatus(Movie movie, ReviewStatus status, Pageable pageable);

    Page<Review> findByMovie(Movie movie, Pageable pageable);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user.profile", "movie",
            "booking.showtime", "booking.showtime.room", "booking.showtime.room.cinema"})
    List<Review> findByUser(User user);

    Optional<Review> findByUserAndMovie(User user, Movie movie);

    boolean existsByUserAndMovie(User user, Movie movie);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Review r WHERE r.booking = :booking AND r.status <> :status")
    boolean existsByBookingAndStatusNot(@Param("booking") Booking booking, @Param("status") ReviewStatus status);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.movie = :movie AND r.status = 'VISIBLE'")
    double averageRatingByMovie(@Param("movie") Movie movie);

    @Modifying
    @Query("UPDATE Review r SET r.status = :status WHERE r.id = :id")
    void setReviewStatus(@Param("id") Long id, @Param("status") ReviewStatus status);
}
