package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.CinemaDashboardResponse;
import com.cinemaai.booking.dto.response.report.ShowtimeIncidentReportResponse;
import com.cinemaai.booking.security.AuthenticatedUser;
import com.cinemaai.booking.security.CinemaSecurityService;
import com.cinemaai.booking.service.AdminBookingService;
import com.cinemaai.booking.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports & Dashboard", description = "Báo cáo doanh thu, tỷ lệ lấp đầy, số vé hủy và hoàn tiền cho Admin & Manager")
public class AdminReportController {

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
