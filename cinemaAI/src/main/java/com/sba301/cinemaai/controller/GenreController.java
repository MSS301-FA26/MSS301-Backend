package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.movie.GenreResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.service.GenreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/genres")
@Tag(name = "Genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    @GetMapping
    public ApiResponse<PageResponse<GenreResponse>> getGenres(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(genreService.getGenres(page, size));
    }

    @GetMapping("/{genreId}")
    public ApiResponse<GenreResponse> getGenre(@PathVariable Long genreId) {
        return ApiResponse.success(genreService.getGenre(genreId));
    }
}
