package com.sba301.cinemaai.dto.response.review;

import com.sba301.cinemaai.entity.Review;
import com.sba301.cinemaai.enums.ReviewStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private Long movieId;
    private String movieTitle;
    private Long bookingId;
    private String bookingCode;
    private Long showtimeId;
    private LocalDateTime showtimeStart;
    private String cinemaName;
    private String roomName;
    private int rating;
    private String comment;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse from(Review review) {
        String fullName = review.getUser().getProfile() != null
                ? review.getUser().getFullName() : null;
        return new ReviewResponse(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getEmail(),
                fullName,
                review.getMovie().getId(),
                review.getMovie().getTitle(),
                review.getBooking() != null ? review.getBooking().getId() : null,
                review.getBooking() != null ? review.getBooking().getBookingCode() : null,
                review.getBooking() != null ? review.getBooking().getShowtime().getId() : null,
                review.getBooking() != null ? review.getBooking().getShowtime().getStartTime() : null,
                review.getBooking() != null ? review.getBooking().getShowtime().getRoom().getCinema().getName() : null,
                review.getBooking() != null ? review.getBooking().getShowtime().getRoom().getName() : null,
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
