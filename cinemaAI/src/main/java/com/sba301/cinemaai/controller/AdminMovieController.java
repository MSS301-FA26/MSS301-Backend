package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.movie.MovieCreateRequest;
import com.sba301.cinemaai.dto.response.movie.MovieResponse;
import com.sba301.cinemaai.dto.request.movie.MovieStatusUpdateRequest;
import com.sba301.cinemaai.dto.request.movie.MovieUpdateRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/movies")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Movies", description = "Admin movie management endpoints - requires ADMIN role")
public class AdminMovieController {

    private final MovieService movieService;

    @GetMapping
    @Operation(summary = "Search movies (Admin)", description = "Search all movies with filters (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movies found successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    public ApiResponse<PageResponse<MovieResponse>> searchMovies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(movieService.searchAdmin(keyword, status, genreId, fromDate, toDate, page, size));
    }

    @GetMapping("/{movieId}")
    @Operation(summary = "Get movie details (Admin)", description = "Get detailed information about a movie (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movie found successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    public ApiResponse<MovieResponse> getMovie(@PathVariable Long movieId) {
        return ApiResponse.success(movieService.getAdmin(movieId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new movie (Admin)", description = "Create a new movie (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Movie created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Movie title already exists")
    })
    public ApiResponse<MovieResponse> createMovie(@Valid @RequestBody MovieCreateRequest request) {
        return ApiResponse.success(movieService.create(request), "Movie created successfully");
    }

    @PutMapping("/{movieId}")
    @Operation(summary = "Update movie (Admin)", description = "Update an existing movie (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movie updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Movie title already exists")
    })
    public ApiResponse<MovieResponse> updateMovie(
            @PathVariable Long movieId,
            @Valid @RequestBody MovieUpdateRequest request
    ) {
        return ApiResponse.success(movieService.update(movieId, request), "Movie updated successfully");
    }

    @PatchMapping("/{movieId}/status")
    @Operation(summary = "Update movie status (Admin)", description = "Update the status of a movie (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movie status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    public ApiResponse<MovieResponse> updateStatus(
            @PathVariable Long movieId,
            @Valid @RequestBody MovieStatusUpdateRequest request
    ) {
        return ApiResponse.success(movieService.updateStatus(movieId, request), "Movie status updated successfully");
    }

    @DeleteMapping("/{movieId}")
    @Operation(summary = "Delete movie (Admin)", description = "Delete a movie by marking it as inactive (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movie deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    public ApiResponse<Void> deleteMovie(@PathVariable Long movieId) {
        movieService.delete(movieId);
        return ApiResponse.success(null, "Movie deleted successfully");
    }
}
