package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.cinema.CinemaResponse;
import com.cinemaai.catalog.dto.response.cinema.RoomResponse;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.service.CinemaService;
import com.cinemaai.catalog.service.RoomService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Cinema")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;
    private final RoomService roomService;

    @GetMapping("/cinema")
    public ApiResponse<CinemaResponse> getCinema() {
        return ApiResponse.success(cinemaService.getPublicCinema());
    }

    @GetMapping("/cinemas")
    public ApiResponse<List<CinemaResponse>> getCinemas() {
        return ApiResponse.success(cinemaService.getPublicCinemas());
    }

    @GetMapping("/cinemas/{cinemaId}")
    public ApiResponse<CinemaResponse> getCinemaById(@PathVariable Long cinemaId) {
        return ApiResponse.success(cinemaService.getCinema(cinemaId));
    }

    @GetMapping("/cinema/rooms")
    public ApiResponse<List<RoomResponse>> getRooms() {
        return ApiResponse.success(roomService.getRooms());
    }

    @GetMapping("/cinemas/{cinemaId}/rooms")
    public ApiResponse<List<RoomResponse>> getRoomsByCinema(@PathVariable Long cinemaId) {
        return ApiResponse.success(roomService.getRoomsByCinema(cinemaId));
    }
}
