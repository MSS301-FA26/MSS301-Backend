package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.ShowtimeBookingSummaryDto;
import java.util.Map;
import java.util.List;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Internal Booking API", description = "API giao tiếp nội bộ giữa các microservice")
public class InternalBookingController {

    private final BookingService bookingService;
    private final com.cinemaai.booking.service.AdminBookingService adminBookingService;

    @Operation(summary = "Lấy chi tiết đơn đặt vé theo ID phục vụ thanh toán (Internal)")
    @GetMapping("/{bookingId}")
    public ApiResponse<BookingResponse> getBookingInternal(@PathVariable Long bookingId) {
        return ApiResponse.success(bookingService.getBookingByIdInternal(bookingId));
    }

    @Operation(summary = "Lấy danh sách các ghế đang bận (BOOKED hoặc HOLDING) của suất chiếu (Internal)")
    @GetMapping("/showtimes/{showtimeId}/occupied-seats")
    public ApiResponse<java.util.List<com.cinemaai.booking.dto.response.OccupiedSeatDto>> getOccupiedSeatsInternal(
            @PathVariable Long showtimeId
    ) {
        return ApiResponse.success(bookingService.getOccupiedSeats(showtimeId));
    }

    @Operation(summary = "Kiểm tra khách hàng đã xem phim hay chưa để cho phép đánh giá (Internal)")
    @GetMapping("/verify-eligibility")
    public ApiResponse<com.cinemaai.booking.dto.response.BookingEligibilityResponse> verifyEligibility(
            @org.springframework.web.bind.annotation.RequestParam Long userId,
            @org.springframework.web.bind.annotation.RequestParam Long movieId
    ) {
        return ApiResponse.success(bookingService.checkReviewEligibility(userId, movieId));
    }

    @Operation(summary = "Đánh dấu vé đã thanh toán (Internal)")
    @org.springframework.web.bind.annotation.PostMapping("/{bookingId}/mark-paid")
    public ApiResponse<BookingResponse> markBookingPaidInternal(
            @PathVariable Long bookingId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String transactionId
    ) {
        return ApiResponse.success(bookingService.markPaidInternal(bookingId, transactionId));
    }

    public record CancelShowtimeInternalRequest(
            String reason,
            String actorRole,
            Long actorUserId
    ) {}

    @Operation(summary = "Huy toan bo ve va hoan tien theo suat chieu bi huy (Internal)")
    @org.springframework.web.bind.annotation.PostMapping("/showtimes/{showtimeId}/cancel-and-refund")
    public ApiResponse<com.cinemaai.booking.dto.response.ShowtimeCancelRefundResultDto> cancelAndRefundShowtimeInternal(
            @PathVariable Long showtimeId,
            @org.springframework.web.bind.annotation.RequestBody(required = false) CancelShowtimeInternalRequest request
    ) {
        String reason = request != null ? request.reason() : "Huy suat chieu do su co ky thuat";
        String role = request != null && request.actorRole() != null ? request.actorRole() : "ADMIN";
        Long userId = request != null ? request.actorUserId() : null;
        return ApiResponse.success(
                adminBookingService.cancelAndRefundShowtime(showtimeId, reason, role, userId),
                "Da hoan tien va huy toan bo ve cua suat chieu"
        );
    }

    @Operation(summary = "L?y s? l??ng v? ?? b?n cho danh s?ch su?t chi?u (Internal)")
    @GetMapping("/showtimes/counts")
    public ApiResponse<Map<Long, Integer>> getSoldTicketCountsInternal(
            @RequestParam List<Long> showtimeIds
    ) {
        return ApiResponse.success(adminBookingService.getSoldTicketCounts(showtimeIds));
    }

    @Operation(summary = "L?y th?ng k? v? v? ti?n c?n ho?n cho su?t chi?u (Internal)")
    @GetMapping("/showtimes/{showtimeId}/summary")
    public ApiResponse<ShowtimeBookingSummaryDto> getShowtimeBookingSummaryInternal(
            @PathVariable Long showtimeId
    ) {
        return ApiResponse.success(adminBookingService.getShowtimeBookingSummary(showtimeId));
    }
}