package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.cinema.CustomerShowtimeSlotResponse;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeResponse;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeSeatMapResponse;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.service.ShowtimeService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/showtimes")
@Tag(name = "Showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @GetMapping("/customer-schedule")
    public ApiResponse<List<CustomerShowtimeSlotResponse>> getCustomerSchedule(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(showtimeService.getCustomerAvailableSlots(movieId, date));
    }

    @GetMapping("/{showtimeId}/resolve")
    public ApiResponse<CustomerShowtimeSlotResponse> resolveCustomerShowtime(@PathVariable Long showtimeId) {
        return ApiResponse.success(showtimeService.resolveCustomerShowtime(showtimeId));
    }

    @GetMapping
    public ApiResponse<PageResponse<ShowtimeResponse>> searchShowtimes(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(showtimeService.searchPublic(movieId, roomId, date, page, size));
    }

    @GetMapping("/{showtimeId}")
    public ApiResponse<ShowtimeResponse> getShowtime(@PathVariable Long showtimeId) {
        return ApiResponse.success(showtimeService.get(showtimeId));
    }

    @GetMapping("/{showtimeId}/seat-map")
    public ApiResponse<ShowtimeSeatMapResponse> getSeatMap(@PathVariable Long showtimeId) {
        return ApiResponse.success(showtimeService.getSeatMap(showtimeId));
    }
}
