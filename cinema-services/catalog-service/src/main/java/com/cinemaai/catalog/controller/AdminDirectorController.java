package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.movie.DirectorRequest;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.movie.DirectorResponse;
import com.cinemaai.catalog.service.DirectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/directors")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Directors", description = "Admin director management endpoints - requires ADMIN role")
public class AdminDirectorController {

    private final DirectorService directorService;

    @GetMapping
    @Operation(summary = "Search directors (Admin)", description = "Search existing directors for movie dropdowns")
    public ApiResponse<PageResponse<DirectorResponse>> getDirectors(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(directorService.searchDirectors(keyword, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get director details by ID (Admin)")
    public ApiResponse<DirectorResponse> getDirector(@PathVariable Long id) {
        return ApiResponse.success(directorService.getDirector(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create director (Admin)", description = "Create a new director (Admin only)")
    public ApiResponse<DirectorResponse> createDirector(@Valid @RequestBody DirectorRequest request) {
        return ApiResponse.success(directorService.create(request), "Director created successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Update director (Admin)", description = "Update director information (Admin only)")
    public ApiResponse<DirectorResponse> updateDirector(
            @PathVariable Long id,
            @Valid @RequestBody DirectorRequest request
    ) {
        return ApiResponse.success(directorService.update(id, request), "Director updated successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete director (Admin)", description = "Delete director when not linked to movies (Admin only)")
    public ApiResponse<Void> deleteDirector(@PathVariable Long id) {
        directorService.delete(id);
        return ApiResponse.success(null, "Director deleted successfully");
    }
}
