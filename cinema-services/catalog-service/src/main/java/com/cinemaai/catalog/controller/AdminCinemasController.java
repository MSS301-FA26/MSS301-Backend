package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.cinema.CinemaRequest;
import com.cinemaai.catalog.dto.response.cinema.CinemaResponse;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.enums.CinemaStatus;
import com.cinemaai.catalog.service.CinemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping({"/api/v1/admin/cinemas", "/api/v1/admin/cinemas/"})
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Cinemas", description = "Admin multi-cinema management endpoints - requires ADMIN role")
public class AdminCinemasController {

    private final CinemaService cinemaService;

    @GetMapping({"", "/"})
    @Operation(summary = "Get all cinemas (Admin)", description = "Get list of all cinemas in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinemas retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    public ApiResponse<List<CinemaResponse>> getCinemas() {
        return ApiResponse.success(cinemaService.getCinemas());
    }

    @PostMapping({"", "/"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create cinema (Admin)", description = "Create a new cinema complex (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Cinema created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cinema name already exists")
    })
    public ApiResponse<CinemaResponse> createCinema(@Valid @RequestBody CinemaRequest request) {
        return ApiResponse.success(cinemaService.create(request), "Cinema created successfully");
    }

    @GetMapping("/{cinemaId}")
    @Operation(summary = "Get cinema by ID (Admin)", description = "Get cinema details by ID (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinema retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<CinemaResponse> getCinema(@PathVariable Long cinemaId) {
        return ApiResponse.success(cinemaService.getCinema(cinemaId));
    }

    @PutMapping("/{cinemaId}")
    @Operation(summary = "Update cinema by ID (Admin)", description = "Update cinema details by ID (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinema updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cinema name already exists")
    })
    public ApiResponse<CinemaResponse> updateCinema(@PathVariable Long cinemaId, @Valid @RequestBody CinemaRequest request) {
        return ApiResponse.success(cinemaService.update(cinemaId, request), "Cinema updated successfully");
    }

    @PatchMapping("/{cinemaId}/status")
    @Operation(summary = "Update cinema status by ID (Admin)", description = "Update status of cinema by ID (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinema status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<CinemaResponse> updateStatus(@PathVariable Long cinemaId, @RequestParam CinemaStatus status) {
        return ApiResponse.success(cinemaService.updateStatus(cinemaId, status), "Cinema status updated successfully");
    }

    @DeleteMapping("/{cinemaId}")
    @Operation(summary = "Delete or deactivate cinema (Admin)", description = "Delete cinema if empty, or deactivate if has rooms (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinema deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<Void> deleteCinema(@PathVariable Long cinemaId) {
        cinemaService.delete(cinemaId);
        return ApiResponse.success(null, "Cinema deleted successfully");
    }
}
