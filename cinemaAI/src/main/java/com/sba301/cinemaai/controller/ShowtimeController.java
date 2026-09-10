package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeSeatMapResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.service.ShowtimeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
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
