package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.review.ReviewRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.review.ReviewResponse;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Movie reviews — public read, customer write, admin moderate")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/movies/{movieId}")
    @Operation(summary = "Get public reviews for a movie", description = "Returns visible reviews sorted by newest. No authentication required.")
    public ApiResponse<PageResponse<ReviewResponse>> getByMovie(
            @Parameter(description = "Movie ID") @PathVariable Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(reviewService.getPublicReviewsByMovie(movieId, page, size));
    }

    @GetMapping("/movies/{movieId}/average-rating")
    @Operation(summary = "Get average rating for a movie", description = "Returns the average rating from all visible reviews.")
    public ApiResponse<Double> getAverageRating(
            @Parameter(description = "Movie ID") @PathVariable Long movieId
    ) {
        return ApiResponse.success(reviewService.getAverageRating(movieId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my reviews", description = "Returns reviews created by the current customer.")
    public ApiResponse<List<ReviewResponse>> getMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(reviewService.getMyReviews(currentUser.getUsername()));
    }

    @PostMapping("/movies/{movieId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Create a review",
            description = "CUSTOMER only. Requires a USED booking for the movie. One review per customer per movie."
    )
    public ApiResponse<ReviewResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Movie ID") @PathVariable Long movieId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success(
                reviewService.createReview(currentUser.getUsername(), movieId, request),
                "Review created successfully"
        );
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update own review", description = "CUSTOMER only. Only VISIBLE reviews can be updated.")
    public ApiResponse<ReviewResponse> update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Review ID") @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success(
                reviewService.updateReview(currentUser.getUsername(), reviewId, request),
                "Review updated successfully"
        );
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Delete own review", description = "CUSTOMER can delete their own review (soft delete).")
    public ApiResponse<Void> deleteMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Review ID") @PathVariable Long reviewId
    ) {
        reviewService.deleteOwnReview(currentUser.getUsername(), reviewId);
        return ApiResponse.success(null, "Review deleted");
    }
}
