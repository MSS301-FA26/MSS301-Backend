package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.review.ReviewRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.review.ReviewResponse;
import java.util.List;

public interface ReviewService {

    ReviewResponse createReview(String userEmail, Long movieId, ReviewRequest request);

    ReviewResponse updateReview(String userEmail, Long reviewId, ReviewRequest request);

    PageResponse<ReviewResponse> getPublicReviewsByMovie(Long movieId, int page, int size);

    List<ReviewResponse> getMyReviews(String userEmail);

    PageResponse<ReviewResponse> getAllReviewsAdmin(Long movieId, String status, int page, int size);

    ReviewResponse hideReview(Long reviewId);

    void deleteOwnReview(String userEmail, Long reviewId);

    void deleteReview(Long reviewId);

    double getAverageRating(Long movieId);
}
