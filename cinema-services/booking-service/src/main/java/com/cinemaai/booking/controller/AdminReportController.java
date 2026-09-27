package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.report.*;
import com.cinemaai.booking.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin - Reports", description = "Báo cáo doanh thu, vé, suất chiếu và sự cố cho Admin")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/revenue")
    @Operation(summary = "Báo cáo doanh thu")
    public ApiResponse<RevenueReportResponse> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getRevenue(from, to));
    }

    @GetMapping("/top-movies")
    @Operation(summary = "Top phim theo vé bán")
    public ApiResponse<List<TopMovieResponse>> topMovies(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(reportService.getTopMovies(from, to, limit));
    }

    @GetMapping("/occupancy")
    @Operation(summary = "Tỷ lệ lấp đầy theo phòng")
    public ApiResponse<List<RoomOccupancyResponse>> occupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getRoomOccupancy(from, to));
    }

    @GetMapping("/occupancy-daily")
    @Operation(summary = "Tỷ lệ lấp đầy theo ngày")
    public ApiResponse<List<DailyOccupancyResponse>> occupancyDaily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getDailyOccupancy(from, to));
    }

    @GetMapping("/showtime-fill")
    @Operation(summary = "Ghế bán vs còn lại theo suất chiếu")
    public ApiResponse<List<ShowtimeFillResponse>> showtimeFill(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "7") int days
    ) {
        return ApiResponse.success(reportService.getShowtimeFill(date, days));
    }

    @GetMapping("/no-shows")
    @Operation(summary = "Báo cáo no-show")
    public ApiResponse<NoShowReportResponse> noShows(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getNoShows(from, to));
    }

    @GetMapping("/peak-hours")
    @Operation(summary = "Giờ cao điểm")
    public ApiResponse<List<PeakHourResponse>> peakHours(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getPeakHours(from, to));
    }

    @GetMapping("/top-seats")
    @Operation(summary = "Ghế bán chạy nhất")
    public ApiResponse<List<TopSeatResponse>> topSeats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long roomId
    ) {
        return ApiResponse.success(reportService.getTopSeats(from, to, roomId));
    }

    @GetMapping("/abandoned")
    @Operation(summary = "Tỷ lệ bỏ giữ ghế")
    public ApiResponse<AbandonedRateResponse> abandoned(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getAbandonedRate(from, to));
    }

    @GetMapping("/concessions")
    @Operation(summary = "Doanh số bắp nước F&B")
    public ApiResponse<ConcessionSalesResponse> concessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getConcessionSales(from, to));
    }

    @GetMapping("/expired-users")
    @Operation(summary = "Người dùng có booking hết hạn")
    public ApiResponse<List<ExpiredUserResponse>> expiredUsers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getExpiredUsers(from, to));
    }

    @GetMapping("/showtime-incidents")
    @Operation(summary = "Báo cáo sự cố suất chiếu")
    public ApiResponse<ShowtimeIncidentReportResponse> showtimeIncidents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getShowtimeIncidents(from, to));
    }
}
