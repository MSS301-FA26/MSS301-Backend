package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.CinemaDashboardResponse;
import com.cinemaai.booking.security.AuthenticatedUser;
import com.cinemaai.booking.service.AdminBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports & Dashboard", description = "Báo cáo doanh thu, tỷ lệ lấp đầy, số vé hủy và hoàn tiền cho Admin & Manager")
public class AdminReportController {

    private final AdminBookingService adminBookingService;

    @Operation(summary = "Lấy dữ liệu Dashboard & Báo cáo tổng hợp theo rạp")
    @GetMapping("/dashboard")
    public ApiResponse<CinemaDashboardResponse> getDashboard(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long cinemaId
    ) {
        CinemaDashboardResponse data = adminBookingService.getDashboardMetrics(user, cinemaId);
        return ApiResponse.success(data, "Lấy dữ liệu báo cáo thống kê thành công");
    }
}
