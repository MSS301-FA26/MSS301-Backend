package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.request.AdminCancelTicketRequest;
import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.ShowtimeBookingSummaryDto;
import java.util.Map;
import java.util.List;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.dto.response.TicketAuditLogResponse;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.security.AuthenticatedUser;
import com.cinemaai.booking.service.AdminBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin Booking Management", description = "Quản lý vé, hủy vé và hoàn tiền cho Admin & Manager")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    @Operation(summary = "Lấy danh sách vé đặt theo rạp và trạng thái")
    @GetMapping
    public ApiResponse<Page<BookingResponse>> getBookings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BookingResponse> page = adminBookingService.getBookings(user, cinemaId, status, search, pageable);
        return ApiResponse.success(page, "Lấy danh sách đơn đặt vé thành công");
    }

    @Operation(summary = "Xem chi tiết một đơn đặt vé")
    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> getBookingById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id
    ) {
        BookingResponse response = adminBookingService.getBookingById(user, id);
        return ApiResponse.success(response, "Lấy chi tiết đơn đặt vé thành công");
    }

    @Operation(summary = "Hủy vé thủ công (Admin & Manager)")
    @PostMapping("/{id}/cancel")
    public ApiResponse<BookingResponse> cancelBooking(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody AdminCancelTicketRequest request
    ) {
        BookingResponse response = adminBookingService.cancelBooking(user, id, request);
        return ApiResponse.success(response, "Hủy vé thành công");
    }

    @Operation(summary = "Lịch sử thao tác hủy và hoàn tiền")
    @GetMapping("/audit-logs")
    public ApiResponse<Page<TicketAuditLogResponse>> getAuditLogs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long cinemaId,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TicketAuditLogResponse> logs = adminBookingService.getAuditLogs(user, cinemaId, pageable);
        return ApiResponse.success(logs, "Lấy lịch sử hủy/hoàn vé thành công");
    }

    @Operation(summary = "Huy va hoan tien toan bo ve cua suat chieu (Admin & Manager)")
    @PostMapping("/showtimes/{showtimeId}/cancel-and-refund")
    public ApiResponse<com.cinemaai.booking.dto.response.ShowtimeCancelRefundResultDto> cancelAndRefundShowtime(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long showtimeId,
            @RequestBody(required = false) AdminCancelTicketRequest request
    ) {
        String reason = request != null && request.reason() != null ? request.reason() : "Huy suat chieu do su co";
        return ApiResponse.success(
                adminBookingService.cancelAndRefundShowtime(user, showtimeId, reason),
                "Da huy va hoan tien toan bo ve cua suat chieu thanh cong"
        );
    }

    @Operation(summary = "L?y s? l??ng v? ?? b?n cho danh s?ch su?t chi?u")
    @GetMapping("/showtimes/counts")
    public ApiResponse<Map<Long, Integer>> getSoldTicketCounts(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam List<Long> showtimeIds
    ) {
        return ApiResponse.success(adminBookingService.getSoldTicketCounts(user, showtimeIds));
    }

    @Operation(summary = "L?y th?ng k? v? v? ti?n c?n ho?n cho su?t chi?u")
    @GetMapping("/showtimes/{showtimeId}/summary")
    public ApiResponse<ShowtimeBookingSummaryDto> getShowtimeBookingSummary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long showtimeId
    ) {
        return ApiResponse.success(adminBookingService.getShowtimeBookingSummary(user, showtimeId));
    }
}
