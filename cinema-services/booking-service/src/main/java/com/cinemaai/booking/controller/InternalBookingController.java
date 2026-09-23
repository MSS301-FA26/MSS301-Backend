package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Internal Booking API", description = "API giao tiếp nội bộ giữa các microservice")
public class InternalBookingController {

    private final BookingService bookingService;

    @Operation(summary = "Lấy chi tiết đơn đặt vé theo ID phục vụ thanh toán (Internal)")
    @GetMapping("/{bookingId}")
    public ApiResponse<BookingResponse> getBookingInternal(@PathVariable Long bookingId) {
        return ApiResponse.success(bookingService.getBookingByIdInternal(bookingId));
    }
}
