package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.CinemaDashboardResponse;
import com.cinemaai.booking.dto.response.report.AbandonedRateResponse;
import com.cinemaai.booking.dto.response.report.AdminAnalyticsResponse;
import com.cinemaai.booking.dto.response.report.ConcessionSalesResponse;
import com.cinemaai.booking.dto.response.report.DailyOccupancyResponse;
import com.cinemaai.booking.dto.response.report.NoShowReportResponse;
import com.cinemaai.booking.dto.response.report.PeakHourResponse;
import com.cinemaai.booking.dto.response.report.RevenueReportResponse;
import com.cinemaai.booking.dto.response.report.RoomOccupancyResponse;
import com.cinemaai.booking.dto.response.report.ShowtimeFillResponse;
import com.cinemaai.booking.dto.response.report.ShowtimeIncidentReportResponse;
import com.cinemaai.booking.dto.response.report.TopMovieResponse;
import com.cinemaai.booking.security.AuthenticatedUser;
import com.cinemaai.booking.security.CinemaSecurityService;
import com.cinemaai.booking.service.AdminAnalyticsService;
import com.cinemaai.booking.service.AdminBookingService;
import com.cinemaai.booking.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin Reports & Dashboard", description = "Báo cáo doanh thu, tỷ lệ lấp đầy, số vé hủy và hoàn tiền cho Admin & Manager")
public class AdminReportController {

    private final AdminAnalyticsService analyticsService;

    public record ReportQuery(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Long cinemaId) {}

    @GetMapping("/analytics")
    public ApiResponse<AdminAnalyticsResponse> analytics(
            @AuthenticationPrincipal AuthenticatedUser user, @ModelAttribute ReportQuery query) {
        return ApiResponse.success(loadAnalytics(user, query));
    }

    @GetMapping("/concessions")
    public ApiResponse<ConcessionSalesResponse> concessions(
            @AuthenticationPrincipal AuthenticatedUser user, @ModelAttribute ReportQuery query) {
        Long cinemaId = cinemaSecurityService.resolveEnforcedCinemaId(user, query.cinemaId(), false);
        return ApiResponse.success(analyticsService.getConcessions(query.from(), query.to(), cinemaId));
    }

    private AdminAnalyticsResponse loadAnalytics(
            AuthenticatedUser user, ReportQuery query) {
        Long cinemaId = cinemaSecurityService.resolveEnforcedCinemaId(user, query.cinemaId(), false);
        return analyticsService.getAnalytics(query.from(), query.to(), cinemaId);
    }

    @GetMapping("/revenue")
    public ApiResponse<RevenueReportResponse> revenue(
            @AuthenticationPrincipal AuthenticatedUser user, @ModelAttribute ReportQuery query) {
        var data = loadAnalytics(user, query);
        return ApiResponse.success(new RevenueReportResponse(data.from(), data.to(), data.summary().totalRevenue(), data.summary().totalTransactions(), data.summary().ticketsSold()));
    }

    @GetMapping("/top-movies")
    public ApiResponse<List<TopMovieResponse>> topMovies(
            @AuthenticationPrincipal AuthenticatedUser user, @ModelAttribute ReportQuery query) {
        var data = loadAnalytics(user, query);
        return ApiResponse.success(data.topMovies());
    }

    @GetMapping("/daily-occupancy")
    public ApiResponse<List<DailyOccupancyResponse>> dailyOccupancy(
            @AuthenticationPrincipal AuthenticatedUser user, @ModelAttribute ReportQuery query) {
        var data = loadAnalytics(user, query);
        return ApiResponse.success(data.dailyOccupancy());
    }

    @GetMapping("/occupancy")
    public ApiResponse<List<RoomOccupancyResponse>> occupancy(
            @AuthenticationPrincipal AuthenticatedUser user, @ModelAttribute ReportQuery query) {
        var data = loadAnalytics(user, query);
        return ApiResponse.success(data.rooms());
    }

    @GetMapping("/showtime-fill")
    public ApiResponse<List<ShowtimeFillResponse>> showtimeFill(
            @AuthenticationPrincipal AuthenticatedUser user, @ModelAttribute ReportQuery query) {
        var data = loadAnalytics(user, query);
        return ApiResponse.success(data.showtimes());
    }

    @GetMapping("/no-shows")
    public ApiResponse<NoShowReportResponse> noShows(
            @AuthenticationPrincipal AuthenticatedUser user, @ModelAttribute ReportQuery query) {
        var data = loadAnalytics(user, query);
        return ApiResponse.success(data.noShows());
    }

    @GetMapping("/peak-hours")
    public ApiResponse<List<PeakHourResponse>> peakHours(
            @AuthenticationPrincipal AuthenticatedUser user, @ModelAttribute ReportQuery query) {
        var data = loadAnalytics(user, query);
        return ApiResponse.success(data.peakHours());
    }

    @GetMapping("/abandoned")
    public ApiResponse<AbandonedRateResponse> abandoned(
            @AuthenticationPrincipal AuthenticatedUser user, @ModelAttribute ReportQuery query) {
        var data = loadAnalytics(user, query);
        return ApiResponse.success(data.abandoned());
    }

    private final AdminBookingService adminBookingService;
    private final ReportService reportService;
    private final CinemaSecurityService cinemaSecurityService;

    @Operation(summary = "Lấy dữ liệu Dashboard & Báo cáo tổng hợp theo rạp")
    @GetMapping("/dashboard")
    public ApiResponse<CinemaDashboardResponse> getDashboard(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long cinemaId
    ) {
        CinemaDashboardResponse data = adminBookingService.getDashboardMetrics(user, cinemaId);
        return ApiResponse.success(data, "Lấy dữ liệu báo cáo thống kê thành công");
    }

    @Operation(summary = "Báo cáo sự cố suất chiếu và hoàn tiền về CineWallet (Admin & Manager)")
    @GetMapping("/showtime-incidents")
    public ApiResponse<ShowtimeIncidentReportResponse> getShowtimeIncidents(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long cinemaId
    ) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(user, cinemaId, false);
        return ApiResponse.success(
                reportService.getShowtimeIncidents(from, to, enforcedCinemaId),
                "Lấy báo cáo sự cố và hoàn tiền thành công"
        );
    }
}
