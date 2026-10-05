package com.cinemaai.catalog.dto.response;

import com.cinemaai.catalog.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long userId,
        String userEmail,
        String userFullName,
        Long movieId,
        String movieTitle,
        String moviePosterUrl,
        Long bookingId,
        int rating,
        String content,
        String status,
        boolean verifiedBooking,
        int reportCount,
        boolean containsSpoiler,
        LocalDateTime hiddenAt,
        String hiddenBy,
        String moderationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewResponse from(Review review) {
        if (review == null) return null;
        String movieTitle = review.getMovie() != null ? review.getMovie().getTitle() : null;
        String moviePoster = review.getMovie() != null ? review.getMovie().getPosterUrl() : null;
        return new ReviewResponse(
                review.getId(),
                review.getUserId(),
                review.getUserEmail(),
                review.getUserFullName(),
                review.getMovie() != null ? review.getMovie().getId() : null,
                movieTitle,
                moviePoster,
                review.getBookingId(),
                review.getRating(),
                review.getContent(),
                review.getStatus().name(),
                review.isVerifiedBooking(),
                review.getReportCount(),
                review.isContainsSpoiler(),
                review.getHiddenAt(),
                review.getHiddenBy(),
                review.getModerationReason(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
