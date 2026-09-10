package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.review.ReviewRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.review.ReviewResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.Review;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.BookingStatus;
import com.sba301.cinemaai.enums.ReviewStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.ReviewRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.ReviewService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService;

    @Transactional
    @Override
    public ReviewResponse createReview(String userEmail, Long movieId, ReviewRequest request) {
        User user = resolveUser(userEmail);
        Movie movie = resolveMovie(movieId);

        Booking usedBooking = resolveReviewBooking(user, movie, request.getBookingId());
        if (usedBooking == null) {
            throw new BadRequestException("You can only review a movie after watching it (booking status must be USED)");
        }
        if (reviewRepository.existsByBookingAndStatusNot(usedBooking, ReviewStatus.DELETED)) {
            throw new BadRequestException("You have already reviewed this ticket");
        }

        Review review = reviewRepository.save(new Review(user, movie, usedBooking, request.getRating(), request.getComment()));
        log.info("User {} created review for movie {}", userEmail, movieId);
        return ReviewResponse.from(review);
    }

    @Transactional
    @Override
    public ReviewResponse updateReview(String userEmail, Long reviewId, ReviewRequest request) {
        User user = resolveUser(userEmail);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found: " + reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Review not found: " + reviewId);
        }
        if (review.getStatus() != ReviewStatus.VISIBLE) {
            throw new BadRequestException("Cannot update a review that is hidden or deleted");
        }
        requireWithinCustomerEditWindow(review);

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        log.info("User {} updated review {}", userEmail, reviewId);
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ReviewResponse> getMyReviews(String userEmail) {
        User user = resolveUser(userEmail);
        return reviewRepository.findByUser(user)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ReviewResponse> getPublicReviewsByMovie(Long movieId, int page, int size) {
        Movie movie = resolveMovie(movieId);
        var pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)),
                Sort.by("createdAt").descending());
        return PageResponse.from(
                reviewRepository.findByMovieAndStatus(movie, ReviewStatus.VISIBLE, pageable)
                        .map(ReviewResponse::from)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ReviewResponse> getAllReviewsAdmin(Long movieId, String status, int page, int size) {




        int pageNumber = Math.max(page, 0);
        int pageSize = Math.max(1, Math.min(size, 10));

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        if (movieId != null) {
            Movie movie = resolveMovie(movieId);
            if (status != null) {
                ReviewStatus reviewStatus = ReviewStatus.valueOf(status.toUpperCase());
                return PageResponse.from(
                        reviewRepository.findByMovieAndStatus(movie, reviewStatus, pageable).map(ReviewResponse::from)
                );
            }
            return PageResponse.from(reviewRepository.findByMovie(movie, pageable).map(ReviewResponse::from));
        }
        if (status != null) {
            ReviewStatus reviewStatus = ReviewStatus.valueOf(status.toUpperCase());
            return PageResponse.from(reviewRepository.findByStatus(reviewStatus, pageable).map(ReviewResponse::from));
        }
        return PageResponse.from(reviewRepository.findAll(pageable).map(ReviewResponse::from));
    }

    @Transactional
    @Override
    public ReviewResponse hideReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new NotFoundException("Review not found: " + reviewId);
        }
        reviewRepository.setReviewStatus(reviewId, ReviewStatus.HIDDEN);
        log.info("Admin hid review {}", reviewId);
        auditLogService.record(AuditActionType.UPDATE, "REVIEW", reviewId, "Ẩn review #" + reviewId);
        return ReviewResponse.from(reviewRepository.findById(reviewId).orElseThrow());
    }

    @Transactional
    @Override
    public void deleteOwnReview(String userEmail, Long reviewId) {
        User user = resolveUser(userEmail);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found: " + reviewId));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Review not found: " + reviewId);
        }
        if (review.getStatus() != ReviewStatus.VISIBLE) {
            throw new BadRequestException("Cannot delete a review that is hidden or deleted");
        }
        requireWithinCustomerEditWindow(review);

        review.setStatus(ReviewStatus.DELETED);
        log.info("User {} deleted own review {}", userEmail, reviewId);
    }

    @Transactional
    @Override
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new NotFoundException("Review not found: " + reviewId);
        }
        reviewRepository.setReviewStatus(reviewId, ReviewStatus.DELETED);
        log.info("Admin deleted (soft) review {}", reviewId);
        auditLogService.record(AuditActionType.DELETE, "REVIEW", reviewId, "Xóa review #" + reviewId);
    }

    @Transactional(readOnly = true)
    @Override
    public double getAverageRating(Long movieId) {
        Movie movie = resolveMovie(movieId);
        return reviewRepository.averageRatingByMovie(movie);
    }

    private void requireWithinCustomerEditWindow(Review review) {
        if (review.getCreatedAt() == null || review.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new BadRequestException("Review can only be changed within 24 hours after creation");
        }
    }

    private Booking resolveReviewBooking(User user, Movie movie, Long bookingId) {
        if (bookingId != null) {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
            if (!booking.getUser().getId().equals(user.getId())
                    || !booking.getShowtime().getMovie().getId().equals(movie.getId())
                    || booking.getStatus() != BookingStatus.USED) {
                throw new BadRequestException("You can only review a movie after watching it (booking status must be USED)");
            }
            return booking;
        }
        return bookingRepository.findUsedBookingsByUserAndMovieOrderByLatest(user, movie)
                .stream()
                .filter((booking) -> !reviewRepository.existsByBookingAndStatusNot(booking, ReviewStatus.DELETED))
                .findFirst()
                .orElse(null);
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
    }

    private Movie resolveMovie(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Movie not found: " + id));
    }
}
