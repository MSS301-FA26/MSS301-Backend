package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.movie.DirectorResponse;
import com.cinemaai.catalog.service.DirectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/directors")
@RequiredArgsConstructor
@Tag(name = "Directors", description = "Public endpoints for browsing directors")
public class DirectorController {

    private final DirectorService directorService;

    @GetMapping
    @Operation(summary = "Search directors", description = "Public endpoint to search directors")
    public ApiResponse<PageResponse<DirectorResponse>> getDirectors(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(directorService.searchDirectors(keyword, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get director details by ID")
    public ApiResponse<DirectorResponse> getDirector(@PathVariable Long id) {
        return ApiResponse.success(directorService.getDirector(id));
    }
}
