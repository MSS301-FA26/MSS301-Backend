package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.cinema.CinemaRequest;
import com.sba301.cinemaai.dto.response.cinema.CinemaResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.enums.CinemaStatus;
import com.sba301.cinemaai.service.CinemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/cinema")
@RequiredArgsConstructor
@Tag(name = "Admin - Cinema", description = "GET is public for displaying cinema information; write operations require ADMIN role")
public class AdminCinemaController {

    private final CinemaService cinemaService;

    @GetMapping
    @Operation(summary = "Get configured cinema", description = "Public endpoint available to every role for displaying cinema name and address")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinema retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not configured")
    })
    public ApiResponse<CinemaResponse> getCinema() {
        return ApiResponse.success(cinemaService.getAdminCinema());
    }

    @PutMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update configured cinema (Admin)", description = "Update the single configured cinema (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinema updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<CinemaResponse> updateCinema(@Valid @RequestBody CinemaRequest request) {
        return ApiResponse.success(cinemaService.update(request), "Cinema updated successfully");
    }

    @PatchMapping("/status")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update configured cinema status (Admin)", description = "Update the status of the single configured cinema (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinema status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<CinemaResponse> updateStatus(@RequestParam CinemaStatus status) {
        return ApiResponse.success(cinemaService.updateStatus(status), "Cinema status updated successfully");
    }

}
