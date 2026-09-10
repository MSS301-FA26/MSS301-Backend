package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.review.ReviewResponse;
import com.sba301.cinemaai.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Review Moderation", description = "Admin review moderation: list, hide, delete")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "List all reviews (admin)", description = "Filter by movieId and/or status. All statuses visible.")
    public ApiResponse<PageResponse<ReviewResponse>> listAll(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(reviewService.getAllReviewsAdmin(movieId, status, page, size));
    }

    @PatchMapping("/{reviewId}/hide")
    @Operation(summary = "Hide a review", description = "Hides a review that violates community guidelines. Review is not deleted.")
    public ApiResponse<ReviewResponse> hide(
            @Parameter(description = "Review ID") @PathVariable Long reviewId
    ) {
        return ApiResponse.success(reviewService.hideReview(reviewId), "Review hidden");
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete a review", description = "Soft-deletes a review. Status becomes DELETED.")
    public ApiResponse<Void> delete(
            @Parameter(description = "Review ID") @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(reviewId);
        return ApiResponse.success(null, "Review deleted");
    }
}
