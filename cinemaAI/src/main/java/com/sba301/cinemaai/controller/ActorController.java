package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.movie.ActorResponse;
import com.sba301.cinemaai.dto.response.movie.MovieResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.service.ActorService;
import com.sba301.cinemaai.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/actors")
@RequiredArgsConstructor
@Tag(name = "Actors", description = "Public actor endpoints")
public class ActorController {

    private final ActorService actorService;
    private final MovieService movieService;

    @GetMapping
    @Operation(summary = "List or search actors", description = "List actors with movie count, optionally filtered by actor name")
    public ApiResponse<PageResponse<ActorResponse>> getActors(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(actorService.searchPublicActors(keyword, page, size));
    }

    @GetMapping("/{actorId}")
    @Operation(summary = "Get actor details", description = "Get actor details with movie count")
    public ApiResponse<ActorResponse> getActor(@PathVariable Long actorId) {
        return ApiResponse.success(actorService.getPublicActor(actorId));
    }

    @GetMapping("/{actorId}/movies")
    @Operation(summary = "Get movies by actor", description = "List public movies that include an actor")
    public ApiResponse<List<MovieResponse>> getMoviesByActor(@PathVariable Long actorId) {
        return ApiResponse.success(movieService.getMoviesByActor(actorId));
    }
}
