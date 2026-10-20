package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.movie.MovieEditRequestResponse;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.MovieEditRequestService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/movie-edit-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class AdminMovieEditRequestController {
    private final MovieEditRequestService service;

    @GetMapping
    public ApiResponse<List<MovieEditRequestResponse>> list(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "PENDING") String status) {
        if ("ALL".equalsIgnoreCase(status)) {
            return ApiResponse.success(service.allRequests());
        }
        return ApiResponse.success(service.pendingRequests());
    }

    @PostMapping("/{requestId}/approve")
    public ApiResponse<MovieEditRequestResponse> approve(@PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.approve(requestId, user.id()));
    }

    @PostMapping("/{requestId}/reject")
    public ApiResponse<MovieEditRequestResponse> reject(@PathVariable Long requestId,
            @RequestBody Rejection request, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.reject(requestId, user.id(), request.reason()));
    }

    public record Rejection(String reason) {}
}
