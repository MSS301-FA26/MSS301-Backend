package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.movie.MovieResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.service.MovieService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/movies")
@Tag(name = "Movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public ApiResponse<PageResponse<MovieResponse>> searchMovies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(movieService.searchPublic(keyword, status, genreId, fromDate, toDate, page, size));
    }

    @GetMapping("/{movieId}")
    public ApiResponse<MovieResponse> getMovie(@PathVariable Long movieId) {
        return ApiResponse.success(movieService.getPublic(movieId));
    }
}
