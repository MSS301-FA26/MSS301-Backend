package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.cinema.InternalShowtimeScopeResponse;
import com.cinemaai.catalog.entity.Showtime;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/catalog/showtimes")
@RequiredArgsConstructor
public class InternalShowtimeController {
    private final ShowtimeService showtimeService;

    @GetMapping("/reporting")
    public ApiResponse<java.util.List<com.cinemaai.catalog.dto.response.cinema.ReportingShowtimeResponse>> getReportingShowtimes(
            @org.springframework.web.bind.annotation.RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @org.springframework.web.bind.annotation.RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long cinemaId) {
        return ApiResponse.success(showtimeService.getReportingShowtimes(from, to, cinemaId));
    }

    @GetMapping("/{showtimeId}/scope")
    public ApiResponse<InternalShowtimeScopeResponse> getScope(@PathVariable Long showtimeId) {
        Showtime showtime = showtimeService.findById(showtimeId);
        if (showtime.getRoom() == null || showtime.getRoom().getCinema() == null
                || showtime.getRoom().getCinema().getId() == null) {
            throw new NotFoundException("Showtime cinema not found");
        }
        return ApiResponse.success(new InternalShowtimeScopeResponse(
                showtime.getId(), showtime.getRoom().getCinema().getId()));
    }
}
