package com.sba301.cinemaai.ai.controller;

import com.sba301.cinemaai.ai.dto.response.RecommendMovieResponse;
import com.sba301.cinemaai.ai.dto.response.RecommendStatsResponse;
import com.sba301.cinemaai.ai.service.RecommendationService;
import com.sba301.cinemaai.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI")
@RestController
@RequestMapping("/api/v1/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService service;

    @GetMapping("/content/{movieId}")
    public ApiResponse<List<RecommendMovieResponse>> content(@PathVariable Long movieId) {
        return ApiResponse.success(service.content(movieId));
    }

    @GetMapping("/collaborative/{userId}")
    public ApiResponse<List<RecommendMovieResponse>> collaborative(@PathVariable Long userId) {
        return ApiResponse.success(service.collaborative(userId));
    }

    @GetMapping("/stats")
    public ApiResponse<RecommendStatsResponse> stats() {
        return ApiResponse.success(service.stats());
    }
}
