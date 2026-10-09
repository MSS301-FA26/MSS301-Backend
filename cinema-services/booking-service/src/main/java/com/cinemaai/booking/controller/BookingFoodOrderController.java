package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.request.FoodOrderRequest;
import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.FoodOrderResponse;
import com.cinemaai.booking.security.AuthenticatedUser;
import com.cinemaai.booking.service.FoodOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/food-orders")
@RequiredArgsConstructor
@Tag(name = "Booking Food Orders", description = "Food orders linked to existing bookings")
public class BookingFoodOrderController {

    private final FoodOrderService foodOrderService;

    @Operation(summary = "Đặt thêm bắp nước cho vé đã đặt")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FoodOrderResponse> createFoodOrderForBooking(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId,
            @Valid @RequestBody FoodOrderRequest request
    ) {
        Long userId = user != null ? user.id() : 1L;
        FoodOrderResponse response = foodOrderService.createForBooking(userId, bookingId, request);
        return ApiResponse.success(response, "Thêm đơn bắp nước vào vé thành công");
    }

    @Operation(summary = "Lấy danh sách bắp nước theo mã vé")
    @GetMapping
    public ApiResponse<List<FoodOrderResponse>> getFoodOrdersByBooking(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(foodOrderService.listByBooking(userId, bookingId));
    }
}