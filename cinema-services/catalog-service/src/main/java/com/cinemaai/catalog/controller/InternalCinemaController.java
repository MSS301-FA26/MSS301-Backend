package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.cinema.InternalCinemaResponse;
import com.cinemaai.catalog.entity.Cinema;
import com.cinemaai.catalog.enums.CinemaStatus;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.repository.CinemaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/cinemas")
@RequiredArgsConstructor
@Tag(name = "Internal - Cinemas", description = "Internal cinema validation endpoints for trusted microservices")
public class InternalCinemaController {

    private final CinemaRepository cinemaRepository;

    @GetMapping("/{cinemaId}")
    @Operation(summary = "Get cinema internal details", description = "Validate cinema existence and status for inter-service communication")
    public ApiResponse<InternalCinemaResponse> getCinema(@PathVariable Long cinemaId) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new NotFoundException("Cụm rạp ID #" + cinemaId + " không tồn tại"));

        boolean isActive = cinema.getStatus() == CinemaStatus.ACTIVE;
        InternalCinemaResponse response = new InternalCinemaResponse(
                cinema.getId(),
                cinema.getName(),
                cinema.getStatus().name(),
                isActive
        );
        return ApiResponse.success(response, "Lấy thông tin rạp thành công");
    }

    @GetMapping("/{cinemaId}/exists")
    @Operation(summary = "Check if cinema exists and is active")
    public ApiResponse<Boolean> existsAndActive(@PathVariable Long cinemaId) {
        boolean active = cinemaRepository.findById(cinemaId)
                .map(c -> c.getStatus() == CinemaStatus.ACTIVE)
                .orElse(false);
        return ApiResponse.success(active);
    }
}
