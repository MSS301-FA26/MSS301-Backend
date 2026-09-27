package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.dto.response.PageResponse;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@Tag(name = "Admin - Bookings", description = "Quản lý vé cho Admin")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "Lấy danh sách vé phân trang cho Admin")
    public ApiResponse<PageResponse<BookingResponse>> getBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(bookingService.getAdminBookings(status, page, size));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Lấy chi tiết vé cho Admin")
    public ApiResponse<BookingResponse> getBooking(@PathVariable Long bookingId) {
        return ApiResponse.success(bookingService.getAdminBooking(bookingId));
    }

    @DeleteMapping("/{bookingId}")
    @Operation(summary = "Hủy vé bởi Admin")
    public ApiResponse<BookingResponse> cancel(
            @PathVariable Long bookingId,
            @RequestParam(required = false) String reason
    ) {
        return ApiResponse.success(bookingService.cancelAdmin(bookingId, reason), "Booking cancelled successfully");
    }
}
