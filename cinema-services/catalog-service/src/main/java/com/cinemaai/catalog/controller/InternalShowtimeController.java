package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.cinema.InternalShowtimeScopeResponse;
import com.cinemaai.catalog.dto.response.cinema.ReportingShowtimeResponse;
import com.cinemaai.catalog.entity.Showtime;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.service.ShowtimeService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/catalog/showtimes")
@RequiredArgsConstructor
public class InternalShowtimeController {
    private final ShowtimeService showtimeService;

    @GetMapping("/reporting")
    public ApiResponse<List<ReportingShowtimeResponse>> getReportingShowtimes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long cinemaId) {
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
