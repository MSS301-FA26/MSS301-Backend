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
@RequestMapping("/api/v1/food-orders")
@RequiredArgsConstructor
@Tag(name = "Food Orders", description = "Standalone Concessions Ordering")
public class StandaloneFoodOrderController {

    private final FoodOrderService foodOrderService;

    @Operation(summary = "Tạo đơn đặt bắp nước lẻ (Không kèm vé)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FoodOrderResponse> createStandaloneOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FoodOrderRequest request
    ) {
        Long userId = user != null ? user.id() : 1L;
        FoodOrderResponse response = foodOrderService.createStandalone(userId, request);
        return ApiResponse.success(response, "Khởi tạo đơn bắp nước thành công");
    }

    @Operation(summary = "Lấy danh sách các đơn bắp nước của tôi")
    @GetMapping("/my")
    public ApiResponse<List<FoodOrderResponse>> getMyFoodOrders(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(foodOrderService.listMine(userId));
    }

    @Operation(summary = "Hủy đơn bắp nước đang chờ thanh toán")
    @DeleteMapping("/{foodOrderId}")
    public ApiResponse<FoodOrderResponse> cancelFoodOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long foodOrderId
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(foodOrderService.cancel(userId, foodOrderId), "Hủy đơn bắp nước thành công");
    }

    @Operation(summary = "Lấy chi tiết đơn bắp nước theo ID")
    @GetMapping("/{foodOrderId}")
    public ApiResponse<FoodOrderResponse> getFoodOrderDetail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long foodOrderId
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(foodOrderService.getByIdAndUser(userId, foodOrderId));
    }

    @Operation(summary = "Đồng bộ trạng thái thanh toán đơn bắp nước")
    @PostMapping("/{foodOrderId}/sync")
    public ApiResponse<FoodOrderResponse> syncPaymentStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long foodOrderId
    ) {
        Long userId = user != null ? user.id() : 1L;
        return ApiResponse.success(foodOrderService.syncPaymentStatus(userId, foodOrderId));
    }
}