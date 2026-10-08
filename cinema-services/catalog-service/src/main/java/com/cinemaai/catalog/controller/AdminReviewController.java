package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.review.ModerateReviewRequest;
import com.cinemaai.catalog.dto.response.AdminReviewStatsResponse;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.ReviewReportResponse;
import com.cinemaai.catalog.dto.response.ReviewResponse;
import com.cinemaai.catalog.security.AuthenticatedUser;
import com.cinemaai.catalog.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Reviews", description = "Quản lý và kiểm duyệt đánh giá phim cho Admin")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Xem danh sách đánh giá phim có bộ lọc, tìm kiếm và phân trang")
    public ApiResponse<PageResponse<ReviewResponse>> listAll(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(reviewService.getAdminReviews(
                movieId, status, rating, verified, search, sort, page, size
        ));
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "Xem chi tiết một đánh giá")
    public ApiResponse<ReviewResponse> getDetail(@PathVariable Long reviewId) {
        return ApiResponse.success(reviewService.getAdminReviewById(reviewId));
    }

    @GetMapping("/stats")
    @Operation(summary = "Lấy các chỉ số KPI thống kê đánh giá phim")
    public ApiResponse<AdminReviewStatsResponse> getStats() {
        return ApiResponse.success(reviewService.getAdminStats());
    }

    @PostMapping("/{reviewId}/hide")
    @Operation(summary = "Ẩn đánh giá vi phạm (kèm lý do)")
    public ApiResponse<ReviewResponse> hide(
            @PathVariable Long reviewId,
            @RequestBody(required = false) ModerateReviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        String adminEmail = admin != null ? admin.email() : "admin@cinepremier.vn";
        Long adminId = admin != null ? admin.id() : null;
        String reason = request != null ? request.reason() : "Nội dung vi phạm tiêu chuẩn cộng đồng";
        return ApiResponse.success(reviewService.hideReview(reviewId, reason, adminEmail, adminId), "Đã ẩn đánh giá");
    }

    @PostMapping("/{reviewId}/restore")
    @Operation(summary = "Khôi phục hiển thị đánh giá")
    public ApiResponse<ReviewResponse> restore(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        String adminEmail = admin != null ? admin.email() : "admin@cinepremier.vn";
        Long adminId = admin != null ? admin.id() : null;
        return ApiResponse.success(reviewService.restoreReview(reviewId, adminEmail, adminId), "Đã khôi phục đánh giá");
    }

    @PostMapping("/{reviewId}/reject")
    @Operation(summary = "Từ chối đánh giá")
    public ApiResponse<ReviewResponse> reject(
            @PathVariable Long reviewId,
            @RequestBody(required = false) ModerateReviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        String adminEmail = admin != null ? admin.email() : "admin@cinepremier.vn";
        Long adminId = admin != null ? admin.id() : null;
        String reason = request != null ? request.reason() : "Đánh giá bị từ chối do vi phạm quy định";
        return ApiResponse.success(reviewService.rejectReview(reviewId, reason, adminEmail, adminId), "Đã từ chối đánh giá");
    }

    @GetMapping("/{reviewId}/reports")
    @Operation(summary = "Xem danh sách các báo cáo vi phạm của một đánh giá")
    public ApiResponse<List<ReviewReportResponse>> getReports(@PathVariable Long reviewId) {
        return ApiResponse.success(reviewService.getReviewReports(reviewId));
    }
}
