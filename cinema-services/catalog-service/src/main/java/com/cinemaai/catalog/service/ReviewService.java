package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.review.CreateReviewRequest;
import com.cinemaai.catalog.dto.request.review.ReportReviewRequest;
import com.cinemaai.catalog.dto.request.review.UpdateReviewRequest;
import com.cinemaai.catalog.dto.response.AdminReviewStatsResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.ReviewEligibilityResponse;
import com.cinemaai.catalog.dto.response.ReviewReportResponse;
import com.cinemaai.catalog.dto.response.ReviewResponse;
import com.cinemaai.catalog.dto.response.ReviewSummaryResponse;

import java.util.List;

public interface ReviewService {

    ReviewEligibilityResponse checkEligibility(Long userId, Long movieId);

    ReviewResponse createReview(Long userId, String email, String fullName, Long movieId, CreateReviewRequest request);

    ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest request);

    PageResponse<ReviewResponse> getPublicReviewsByMovie(Long movieId, int page, int size);

    ReviewSummaryResponse getReviewSummaryByMovie(Long movieId);

    Double getAverageRating(Long movieId);

    List<ReviewResponse> getMyReviews(Long userId);

    void reportReview(Long userId, String userEmail, Long reviewId, ReportReviewRequest request);

    PageResponse<ReviewResponse> getAdminReviews(
            Long movieId,
            String status,
            Integer rating,
            Boolean verified,
            String search,
            String sort,
            int page,
            int size
    );

    ReviewResponse getAdminReviewById(Long reviewId);

    AdminReviewStatsResponse getAdminStats();

    ReviewResponse hideReview(Long reviewId, String reason, String adminEmail, Long adminId);

    ReviewResponse restoreReview(Long reviewId, String adminEmail, Long adminId);

    ReviewResponse rejectReview(Long reviewId, String reason, String adminEmail, Long adminId);

    List<ReviewReportResponse> getReviewReports(Long reviewId);
}
