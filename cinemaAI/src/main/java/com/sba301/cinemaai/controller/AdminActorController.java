package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.movie.ActorRequest;
import com.sba301.cinemaai.dto.response.movie.ActorResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.service.ActorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/admin/actors")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Actors", description = "Admin actor management endpoints - requires ADMIN role")
public class AdminActorController {

    private final ActorService actorService;

    @GetMapping
    @Operation(summary = "List or search actors (Admin)", description = "Search existing actors by name for movie dropdowns")
    public ApiResponse<PageResponse<ActorResponse>> getActors(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(actorService.searchAdminActors(keyword, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create actor (Admin)", description = "Create a new actor")
    public ApiResponse<ActorResponse> createActor(@Valid @RequestBody ActorRequest request) {
        return ApiResponse.success(actorService.create(request), "Actor created successfully");
    }

    @PutMapping("/{actorId}")
    @Operation(summary = "Update actor (Admin)", description = "Update actor information")
    public ApiResponse<ActorResponse> updateActor(
            @PathVariable Long actorId,
            @Valid @RequestBody ActorRequest request
    ) {
        return ApiResponse.success(actorService.update(actorId, request), "Actor updated successfully");
    }

    @DeleteMapping("/{actorId}")
    @Operation(summary = "Delete actor (Admin)", description = "Delete actor when not linked to movies")
    public ApiResponse<Void> deleteActor(@PathVariable Long actorId) {
        actorService.delete(actorId);
        return ApiResponse.success(null, "Actor deleted successfully");
    }
}
