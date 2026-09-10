package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.report.AbandonedRateResponse;
import com.sba301.cinemaai.dto.response.report.ConcessionSalesResponse;
import com.sba301.cinemaai.dto.response.report.ExpiredUserResponse;
import com.sba301.cinemaai.dto.response.report.NoShowReportResponse;
import com.sba301.cinemaai.dto.response.report.PeakHourResponse;
import com.sba301.cinemaai.dto.response.report.RevenueReportResponse;
import com.sba301.cinemaai.dto.response.report.RoomOccupancyResponse;
import com.sba301.cinemaai.dto.response.report.ShowtimeFillResponse;
import com.sba301.cinemaai.dto.response.report.TopMovieResponse;
import com.sba301.cinemaai.dto.response.report.TopSeatResponse;
import com.sba301.cinemaai.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Reports", description = "Revenue, ticket sales, top movies and room occupancy reports")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/revenue")
    @Operation(
            summary = "Revenue report",
            description = "Total revenue, transaction count and tickets sold in the given date range. Defaults to current month."
    )
    public ApiResponse<RevenueReportResponse> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getRevenue(from, to));
    }

    @GetMapping("/top-movies")
    @Operation(
            summary = "Top movies by tickets sold",
            description = "Lists movies ranked by tickets sold descending. Returns up to `limit` rows (max 50)."
    )
    public ApiResponse<List<TopMovieResponse>> topMovies(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(reportService.getTopMovies(from, to, limit));
    }

    @GetMapping("/occupancy")
    @Operation(
            summary = "Room occupancy rate",
            description = "Tickets sold vs room capacity per room in the date range."
    )
    public ApiResponse<List<RoomOccupancyResponse>> occupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getRoomOccupancy(from, to));
    }

    @GetMapping("/occupancy-daily")
    @Operation(summary = "Daily occupancy rate",
            description = "Seats sold vs total capacity per DAY (all rooms combined) — fill-rate trend over time. Defaults to last 30 days.")
    public ApiResponse<List<com.sba301.cinemaai.dto.response.report.DailyOccupancyResponse>> occupancyDaily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getDailyOccupancy(from, to));
    }

    @GetMapping("/showtime-fill")
    @Operation(summary = "Seats sold vs remaining per showtime",
            description = "Forward-looking list of showtimes with capacity, sold and remaining seats. Defaults to 7 days from today.")
    public ApiResponse<List<ShowtimeFillResponse>> showtimeFill(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "7") int days
    ) {
        return ApiResponse.success(reportService.getShowtimeFill(date, days));
    }

    @GetMapping("/no-shows")
    @Operation(summary = "No-show report",
            description = "PAID bookings whose showtime already ended but were never checked in.")
    public ApiResponse<NoShowReportResponse> noShows(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getNoShows(from, to));
    }

    @GetMapping("/peak-hours")
    @Operation(summary = "Peak-hours histogram",
            description = "Tickets sold grouped by showtime start hour (0-23) — use to spot peak hours for pricing.")
    public ApiResponse<List<PeakHourResponse>> peakHours(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getPeakHours(from, to));
    }

    @GetMapping("/top-seats")
    @Operation(summary = "Most-purchased seats",
            description = "Seats ranked by purchase count (PAID/USED) — use to spot popular seat positions; optional roomId filter.")
    public ApiResponse<List<TopSeatResponse>> topSeats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long roomId
    ) {
        return ApiResponse.success(reportService.getTopSeats(from, to, roomId));
    }

    @GetMapping("/abandoned")
    @Operation(summary = "Abandoned booking rate",
            description = "EXPIRED (held but never paid) vs paid bookings in the range.")
    public ApiResponse<AbandonedRateResponse> abandoned(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getAbandonedRate(from, to));
    }

    @GetMapping("/concessions")
    @Operation(summary = "Concession sales",
            description = "Quantity and revenue of food items/combos sold (including post-payment food orders).")
    public ApiResponse<ConcessionSalesResponse> concessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getConcessionSales(from, to));
    }

    @GetMapping("/expired-users")
    @Operation(summary = "Users with expired (unpaid) bookings",
            description = "Users who held seats but never paid — candidates for goodwill loyalty points.")
    public ApiResponse<List<ExpiredUserResponse>> expiredUsers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(reportService.getExpiredUsers(from, to));
    }

    @GetMapping("/showtime-incidents")
    @Operation(summary = "Showtime cancellation incident & refund report",
            description = "Detailed report of cancelled showtimes, affected bookings/users, refunded amounts and reasons.")
    public ApiResponse<com.sba301.cinemaai.dto.response.report.ShowtimeIncidentReportResponse> showtimeIncidents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long showtimeId
    ) {
        return ApiResponse.success(reportService.getShowtimeIncidents(from, to, showtimeId));
    }
}
