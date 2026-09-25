package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.movie.MovieResponse;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.enums.MovieStatus;
import com.cinemaai.catalog.service.MovieService;
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
    private final com.cinemaai.catalog.service.ShowtimeService showtimeService;

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

    @GetMapping("/{movieId}/available-showtimes")
    public ApiResponse<java.util.List<com.cinemaai.catalog.dto.response.cinema.CustomerShowtimeSlotResponse>> getAvailableShowtimes(
            @PathVariable Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(showtimeService.getCustomerAvailableSlots(movieId, date));
    }
}
