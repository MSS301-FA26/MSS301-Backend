package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.request.CheckoutBookingRequest;
import com.cinemaai.booking.dto.request.HoldSeatsRequest;
import com.cinemaai.booking.dto.request.UpdateHoldingBookingRequest;
import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.security.AuthenticatedUser;
import com.cinemaai.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Core", description = "Quản lý luồng đặt vé, giữ ghế 3 phút và thanh toán")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Giữ ghế 3 phút có kiểm soát tương tranh (Seat Hold Concurrency)")
    @PostMapping("/hold")
    public ApiResponse<BookingResponse> holdSeats(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody HoldSeatsRequest request
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(bookingService.holdSeats(userId, request), "Giữ ghế thành công (Hiệu lực 3 phút)");
    }

    @Operation(summary = "Checkout đơn vé và đóng băng Snapshot bất biến")
    @PostMapping("/{bookingId}/checkout")
    public ApiResponse<BookingResponse> checkout(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId,
            @Valid @RequestBody CheckoutBookingRequest request
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(bookingService.checkout(userId, bookingId, request), "Checkout thành công. Đơn chờ thanh toán.");
    }

    @Operation(summary = "Cập nhật bắp nước và phân loại vé khi đang giữ chỗ (Hỗ trợ FE updateHoldingBooking)")
    @PutMapping("/{bookingId}/items")
    public ApiResponse<BookingResponse> updateItems(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId,
            @RequestBody UpdateHoldingBookingRequest request
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(bookingService.updateHoldingItems(userId, bookingId, request), "Cập nhật thông tin vé thành công");
    }

    @Operation(summary = "Lấy danh sách vé đã đặt của người dùng hiện tại")
    @GetMapping
    public ApiResponse<Page<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = user != null ? user.id() : 1L;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(bookingService.getMyBookings(userId, pageRequest));
    }

    @Operation(summary = "Xem chi tiết một đơn đặt vé")
    @GetMapping("/{bookingId}")
    public ApiResponse<BookingResponse> getBookingDetail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(bookingService.getBookingById(userId, bookingId));
    }

    @Operation(summary = "Hủy giữ chỗ / giải phóng ghế ngay lập tức")
    @DeleteMapping("/{bookingId}")
    public ApiResponse<BookingResponse> cancelBooking(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(bookingService.cancelBooking(userId, bookingId), "Hủy giữ chỗ thành công");
    }

    @Operation(summary = "Tra cứu vé theo mã đặt vé (Nội bộ / Nhân viên)")
    @GetMapping("/code/{bookingCode}")
    public ApiResponse<BookingResponse> getByCode(@PathVariable String bookingCode) {
        return ApiResponse.success(bookingService.getBookingByCode(bookingCode));
    }
}
