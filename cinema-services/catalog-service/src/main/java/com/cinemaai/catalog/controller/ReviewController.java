package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.review.CreateReviewRequest;
import com.cinemaai.catalog.dto.request.review.ReportReviewRequest;
import com.cinemaai.catalog.dto.request.review.UpdateReviewRequest;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.ReviewEligibilityResponse;
import com.cinemaai.catalog.dto.response.ReviewResponse;
import com.cinemaai.catalog.dto.response.ReviewSummaryResponse;
import com.cinemaai.catalog.exception.BadRequestException;
import com.cinemaai.catalog.security.AuthenticatedUser;
import com.cinemaai.catalog.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Customer - Reviews", description = "Đánh giá phim: xem công khai, kiểm tra điều kiện, viết và sửa đánh giá")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/movies/{movieId}")
    @Operation(summary = "Lấy danh sách đánh giá công khai của phim")
    public ApiResponse<PageResponse<ReviewResponse>> getPublicReviews(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(reviewService.getPublicReviewsByMovie(movieId, page, size));
    }

    @GetMapping("/movies/{movieId}/summary")
    @Operation(summary = "Lấy tổng quan đánh giá phim (điểm trung bình, phân bổ điểm, tỷ lệ xác minh vé)")
    public ApiResponse<ReviewSummaryResponse> getReviewSummary(@PathVariable Long movieId) {
        return ApiResponse.success(reviewService.getReviewSummaryByMovie(movieId));
    }

    @GetMapping("/movies/{movieId}/average-rating")
    @Operation(summary = "Lấy điểm trung bình đánh giá của phim")
    public ApiResponse<Double> getAverageRating(@PathVariable Long movieId) {
        return ApiResponse.success(reviewService.getAverageRating(movieId));
    }

    @GetMapping("/movies/{movieId}/eligibility")
    @Operation(summary = "Kiểm tra điều kiện viết đánh giá của khách hàng")
    public ApiResponse<ReviewEligibilityResponse> checkEligibility(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long movieId
    ) {
        if (user == null) {
            return ApiResponse.success(new ReviewEligibilityResponse(
                    false, false, "UNAUTHENTICATED", "Vui lòng đăng nhập để kiểm tra điều kiện đánh giá.",
                    null, null, null, null, null
            ));
        }
        return ApiResponse.success(reviewService.checkEligibility(user.id(), movieId));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy danh sách đánh giá của tôi")
    public ApiResponse<List<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        if (user == null) {
            return ApiResponse.success(List.of());
        }
        return ApiResponse.success(reviewService.getMyReviews(user.id()));
    }

    @PostMapping("/movies/{movieId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gửi đánh giá phim mới (yêu cầu đã xem phim)")
    public ApiResponse<ReviewResponse> createReview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long movieId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        if (user == null) {
            throw new BadRequestException("Vui lòng đăng nhập để viết đánh giá.");
        }
        ReviewResponse created = reviewService.createReview(user.id(), user.email(), user.getUsername(), movieId, request);
        return ApiResponse.success(created, "Đánh giá của bạn đã được xuất bản thành công!");
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Chỉnh sửa đánh giá của mình (trong vòng 24h)")
    public ApiResponse<ReviewResponse> updateReview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        if (user == null) {
            throw new BadRequestException("Vui lòng đăng nhập để chỉnh sửa đánh giá.");
        }
        ReviewResponse updated = reviewService.updateReview(user.id(), reviewId, request);
        return ApiResponse.success(updated, "Cập nhật đánh giá thành công!");
    }

    @PostMapping("/{reviewId}/report")
    @Operation(summary = "Báo cáo vi phạm đánh giá")
    public ApiResponse<Void> reportReview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReportReviewRequest request
    ) {
        if (user == null) {
            throw new BadRequestException("Vui lòng đăng nhập để gửi báo cáo.");
        }
        reviewService.reportReview(user.id(), user.email(), reviewId, request);
        return ApiResponse.success(null, "Cảm ơn bạn đã báo cáo. Chúng tôi sẽ kiểm duyệt nội dung này.");
    }
}
