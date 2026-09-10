package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.movie.GenreRequest;
import com.sba301.cinemaai.dto.response.movie.GenreResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.service.GenreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/genres")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Genres", description = "Admin genre management endpoints - requires ADMIN role")
public class AdminGenreController {

    private final GenreService genreService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new genre (Admin)", description = "Create a new genre (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Genre created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Genre already exists")
    })
    public ApiResponse<GenreResponse> createGenre(@Valid @RequestBody GenreRequest request) {
        return ApiResponse.success(genreService.create(request), "Genre created successfully");
    }

    @PutMapping("/{genreId}")
    @Operation(summary = "Update genre (Admin)", description = "Update an existing genre (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Genre updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Genre not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Genre already exists")
    })
    public ApiResponse<GenreResponse> updateGenre(
            @PathVariable Long genreId,
            @Valid @RequestBody GenreRequest request
    ) {
        return ApiResponse.success(genreService.update(genreId, request), "Genre updated successfully");
    }

    @DeleteMapping("/{genreId}")
    @Operation(summary = "Delete genre (Admin)", description = "Delete a genre (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Genre deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Genre not found")
    })
    public ApiResponse<Void> deleteGenre(@PathVariable Long genreId) {
        genreService.delete(genreId);
        return ApiResponse.success(null, "Genre deleted successfully");
    }
}
