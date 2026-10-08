package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.movie.MovieEditProposalRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.movie.MovieEditRequestResponse;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.MovieEditRequestService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager/movie-edit-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ManagerMovieEditRequestController {
    private final MovieEditRequestService service;

    @GetMapping
    public ApiResponse<List<MovieEditRequestResponse>> myRequests(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.myRequests(user.id()));
    }

    @PostMapping("/movies/{movieId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MovieEditRequestResponse> submit(@PathVariable Long movieId,
            @Valid @RequestBody MovieEditProposalRequest proposal,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.submit(movieId, user.id(), proposal));
    }
}
