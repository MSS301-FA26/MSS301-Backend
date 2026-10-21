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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Customer concession orders, standalone or linked to a booking")
public class OrderController {

    private final FoodOrderService foodOrderService;

    @PostMapping("/api/v1/food-orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo đơn sản phẩm tại quầy không kèm vé")
    public ApiResponse<FoodOrderResponse> createStandaloneOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FoodOrderRequest request
    ) {
        FoodOrderResponse response = foodOrderService.createStandalone(user.id(), request);
        return ApiResponse.success(response, "Khởi tạo đơn bắp nước thành công");
    }

    @GetMapping("/api/v1/food-orders/my")
    @Operation(summary = "Lấy danh sách đơn sản phẩm tại quầy của tôi")
    public ApiResponse<List<FoodOrderResponse>> getMyFoodOrders(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(foodOrderService.listMine(user.id()));
    }

    @DeleteMapping("/api/v1/food-orders/{foodOrderId}")
    @Operation(summary = "Hủy đơn sản phẩm tại quầy đang chờ thanh toán")
    public ApiResponse<FoodOrderResponse> cancelFoodOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long foodOrderId
    ) {
        return ApiResponse.success(
                foodOrderService.cancel(user.id(), foodOrderId),
                "Hủy đơn bắp nước thành công"
        );
    }

    @GetMapping("/api/v1/food-orders/{foodOrderId}")
    @Operation(summary = "Lấy chi tiết đơn sản phẩm tại quầy theo ID")
    public ApiResponse<FoodOrderResponse> getFoodOrderDetail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long foodOrderId
    ) {
        return ApiResponse.success(foodOrderService.getByIdAndUser(user.id(), foodOrderId));
    }

    @PostMapping("/api/v1/food-orders/{foodOrderId}/sync")
    @Operation(summary = "Đồng bộ trạng thái thanh toán đơn sản phẩm tại quầy")
    public ApiResponse<FoodOrderResponse> syncPaymentStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long foodOrderId
    ) {
        return ApiResponse.success(foodOrderService.syncPaymentStatus(user.id(), foodOrderId));
    }

    @PostMapping("/api/v1/bookings/{bookingId}/food-orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đặt thêm sản phẩm tại quầy cho vé đã đặt")
    public ApiResponse<FoodOrderResponse> createFoodOrderForBooking(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId,
            @Valid @RequestBody FoodOrderRequest request
    ) {
        FoodOrderResponse response = foodOrderService.createForBooking(user.id(), bookingId, request);
        return ApiResponse.success(response, "Thêm đơn bắp nước vào vé thành công");
    }

    @GetMapping("/api/v1/bookings/{bookingId}/food-orders")
    @Operation(summary = "Lấy danh sách sản phẩm tại quầy theo mã vé")
    public ApiResponse<List<FoodOrderResponse>> getFoodOrdersByBooking(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId
    ) {
        return ApiResponse.success(foodOrderService.listByBooking(user.id(), bookingId));
    }
}
