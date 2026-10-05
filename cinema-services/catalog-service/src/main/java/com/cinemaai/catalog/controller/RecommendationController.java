package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.recommendation.RecommendMovieResponse;
import com.cinemaai.catalog.dto.response.recommendation.RecommendStatsResponse;
import com.cinemaai.catalog.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/recommendation", "/api/v1/recommendations"})
@Tag(name = "Recommendation AI", description = "Gợi ý phim thông minh (Collaborative + Content-Based)")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "Gợi ý phim theo nội dung tương tự (Content-based)")
    @GetMapping("/content/{movieId}")
    public ApiResponse<List<RecommendMovieResponse>> content(@PathVariable Long movieId) {
        return ApiResponse.success(recommendationService.content(movieId), "Lấy danh sách phim tương tự thành công");
    }

    @Operation(summary = "Gợi ý phim cá nhân hóa theo người dùng (Collaborative filtering)")
    @GetMapping({"/collaborative/{userId}", "/user/{userId}"})
    public ApiResponse<List<RecommendMovieResponse>> collaborative(@PathVariable Long userId) {
        return ApiResponse.success(recommendationService.collaborative(userId), "Lấy danh sách phim gợi ý thành công");
    }

    @Operation(summary = "Thống kê hệ thống gợi ý AI")
    @GetMapping("/stats")
    public ApiResponse<RecommendStatsResponse> stats() {
        return ApiResponse.success(recommendationService.stats(), "Lấy thống kê gợi ý thành công");
    }
}
