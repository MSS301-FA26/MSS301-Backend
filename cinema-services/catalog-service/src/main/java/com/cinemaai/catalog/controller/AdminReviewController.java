package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "Admin - Reviews", description = "Quản lý đánh giá phim cho Admin")
public class AdminReviewController {

    @GetMapping
    @Operation(summary = "Xem danh sách đánh giá phim")
    public ApiResponse<PageResponse<ReviewResponse>> listAll(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(new PageResponse<>(List.of(), page, size, 0L, 1, true, true));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Xóa đánh giá")
    public ApiResponse<Void> delete(@PathVariable Long reviewId) {
        return ApiResponse.success(null, "Review deleted");
    }

    @PatchMapping("/{reviewId}/hide")
    @Operation(summary = "Ẩn đánh giá")
    public ApiResponse<Void> hide(@PathVariable Long reviewId) {
        return ApiResponse.success(null, "Review hidden");
    }
}
