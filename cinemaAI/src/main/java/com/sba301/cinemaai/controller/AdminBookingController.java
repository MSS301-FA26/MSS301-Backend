package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.booking.BookingResponse;
import com.sba301.cinemaai.dto.request.booking.CheckInRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.enums.BookingStatus;
import com.sba301.cinemaai.service.BookingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@Tag(name = "Admin - Bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    public ApiResponse<PageResponse<BookingResponse>> getBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(bookingService.getAdminBookings(status, page, size));
    }

    @GetMapping("/{bookingId}")
    public ApiResponse<BookingResponse> getBooking(@PathVariable Long bookingId) {
        return ApiResponse.success(bookingService.getAdminBooking(bookingId));
    }

    @DeleteMapping("/{bookingId}")
    public ApiResponse<BookingResponse> cancel(
            @PathVariable Long bookingId,
            @RequestParam(required = false) String reason
    ) {
        return ApiResponse.success(bookingService.cancelAdmin(bookingId, reason), "Booking cancelled successfully");
    }

    @PostMapping("/{bookingId}/check-in")
    public ApiResponse<BookingResponse> checkIn(
            @PathVariable Long bookingId,
            @Valid @RequestBody(required = false) CheckInRequest request
    ) {
        String qrCode = request == null ? null : request.qrCode();
        return ApiResponse.success(bookingService.checkInAdmin(bookingId, qrCode), "Booking checked in successfully");
    }
}
